package com.yiqiniu.easytrans.idempotent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.idempotent.IdempotentHelper.IdempotentPo;
import com.yiqiniu.easytrans.idempotent.exception.ResultWrapperExeception;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ObjectDigestUtil;

/**
 * this idempotent implement is suitable for ROLLBACKable business only.
 * if business can not be roll back, this idempotent implement may cause inconsistent
 */
@Order(10)
public class DefaultIdempotentHandlerFilter implements IdempotentHandlerFilter {
	
	private IdempotentHelper helper;
	private ObjectSerializer serializer;
	private String appId;
	private IdempotentTransactionDefinition transactionDefinition;
	
	public DefaultIdempotentHandlerFilter(String appId, IdempotentHelper helper, ObjectSerializer serializer, IdempotentTransactionDefinition transactionDefinition) {
		super();
		this.helper = helper;
		this.serializer = serializer;
		this.appId = appId;
		this.transactionDefinition = transactionDefinition;
	}

	@Override
	public EasyTransResult invoke(final EasyTransFilterChain filterChain, final Map<String,Object> header, final EasyTransRequest<?, ?> request) {
		
		if(RpcBusinessProvider.IDENPOTENT_TYPE_FRAMEWORK == helper.getIdempotentType(request)){
			
			//start transaction，require
			PlatformTransactionManager transactionManager = helper.getTransactionManager(filterChain,request);
			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager, transactionDefinition.getTransactionDefinition(filterChain, header, request));
			EasyTransResult result = null;
			try {
				result = transactionTemplate.execute(new TransactionCallback<EasyTransResult>() {

							@Override
							public EasyTransResult doInTransaction(TransactionStatus status) {
								
								IdempotentPo idempotentPo = helper.getIdempotentPo(filterChain, header, request);
								ExecuteOrder executeOrder = helper.getExecuteOrder(filterChain.getAppId(), filterChain.getBusCode(), filterChain.getInnerMethodName());
								
								byte[] serialization = serializer.serialization(request);
								String objectMD5 = ObjectDigestUtil.MD5Encode(serialization);
								
								EasyTransResult innerResult = filterResult(idempotentPo, objectMD5, executeOrder);
								
								
								if(innerResult == null){
									/**
									 *  execute business
									 */
									innerResult = filterChain.invokeFilterChain(header,request);
									if (innerResult.hasException()) {
										// throw an exception to roll back
										throw new ResultWrapperExeception(innerResult);
									}
								}

								
								/**
								 * save execute result
								 */
								Date dateNow = new Date();
								if(idempotentPo == null){
									idempotentPo = new IdempotentPo();
									TransactionId transactionId = (TransactionId) header.get(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
									Integer callSeq = (Integer) header.get(EasytransConstant.CallHeadKeys.CALL_SEQ);
									
									idempotentPo.setSrcAppId(transactionId.getAppId());
									idempotentPo.setSrcBusCode(transactionId.getBusCode());
									idempotentPo.setSrcTrxId(transactionId.getTrxId());
									idempotentPo.setAppId(filterChain.getAppId());
									idempotentPo.setBusCode(filterChain.getBusCode());
									idempotentPo.setCallSeq(callSeq);
									idempotentPo.setHandler(appId);
									
									idempotentPo.setCalledMethods(filterChain.getInnerMethodName());
									idempotentPo.setMd5(objectMD5);
									if(executeOrder != null && executeOrder.isSynchronousMethod()){
										idempotentPo.setSyncMethodResult(serializer.serialization(innerResult));
									}
									
									idempotentPo.setCreateTime(dateNow);
									idempotentPo.setUpdateTime(dateNow);
									idempotentPo.setLockVersion(1);
									
									helper.saveIdempotentPo(filterChain,idempotentPo);
								}else{
									
									String calledMethodString = idempotentPo.getCalledMethods();
									List<String>  executedMethods = Arrays.asList(calledMethodString.split(","));
									if(!executedMethods.contains(filterChain.getInnerMethodName())){
										idempotentPo.setCalledMethods(calledMethodString + "," + filterChain.getInnerMethodName());
										if(executeOrder != null && executeOrder.isSynchronousMethod()){
											idempotentPo.setSyncMethodResult(serializer.serialization(innerResult));
										}
										idempotentPo.setUpdateTime(dateNow);
										helper.updateIdempotentPo(filterChain, idempotentPo);
									}
								}
								
								// return the executed result
								return innerResult;
							}
							
							
							public EasyTransResult filterResult(IdempotentPo idempotentPo,String objectMD5,ExecuteOrder executeOrder){
								
								List<String> executedMethods = new ArrayList<String>();
								if(idempotentPo != null){
									String calledMethodString = idempotentPo.getCalledMethods();
									executedMethods.addAll(Arrays.asList(calledMethodString.split(",")));
									
									/**
									 * check parameters
									 */
									if(!objectMD5.equals(idempotentPo.getMd5())){
										return new EasyTransResult(new RuntimeException("duplicated requestId with different params!" + request));
									}
									
									/**
									 * idempotent call handling
									 */
									if(executedMethods.contains(filterChain.getInnerMethodName())){
										//already executed
										//return the previous result
										if(executeOrder != null && executeOrder.isSynchronousMethod()){
											return (EasyTransResult)serializer.deserialize(idempotentPo.getSyncMethodResult());
										}else{
											return new EasyTransResult(null);
										}
									}
								}
								
								
								/**
								 * call order handling
								 */
								if(executeOrder != null){
									for(String doNotExecuteFlag : executeOrder.doNotExecuteAfter()){
										if(executedMethods.contains(doNotExecuteFlag)){
											return new EasyTransResult(new RuntimeException(doNotExecuteFlag + " already executed,can not execute " + filterChain.getInnerMethodName() +" " + request));
										}
									}
									for(String passFlag : executeOrder.ifNotExecutedReturnDirectly()){
										if(!executedMethods.contains(passFlag)){
											return new EasyTransResult(null);//return null as a result
										}
									}
								}
								
								return null;
							}
							
							
						});
			} catch (ResultWrapperExeception e) {
				result = e.getResult();
			} catch (Throwable e) {
				if (result == null) {
					result = new EasyTransResult(e);
				}
			}
			
			return result;
		}else{
			return filterChain.invokeFilterChain(header,request);
		}
	}

}
