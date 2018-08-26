package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.context.event.DemiLogEventHandler;
import com.yiqiniu.easytrans.core.EasyTransSynchronizer;
import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.core.LogProcessor;
import com.yiqiniu.easytrans.core.RemoteServiceCaller;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.saga.PreSagaTccCallContent;
import com.yiqiniu.easytrans.log.vo.saga.SagaTccCallCancelledContent;
import com.yiqiniu.easytrans.log.vo.saga.SagaTccCallConfirmedContent;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.SerializableVoid;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethod;
import com.yiqiniu.easytrans.util.ReflectUtil;

@RelativeInterface(SagaTccMethod.class)
public class SagaTccMethodExecutor implements EasyTransExecutor,LogProcessor,DemiLogEventHandler {

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller rpcClient;
	@SuppressWarnings("rawtypes")
	private Future nullObject;
	
	public SagaTccMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.rpcClient = rpcClient;
		
		
		CompletableFuture<Object> completableFuture = new CompletableFuture<>();
		completableFuture.complete(SerializableVoid.SINGLETON);
		this.nullObject =  completableFuture;
	}

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private static final String TRY_METHOD_NAME = "sagaTry";
	private static final String CONFIRM_METHOD_NAME = "sagaConfirm";
	private static final String CANCEL_METHOD_NAME = "sagaCancel";
	

	
	@SuppressWarnings("unchecked")
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer callSeq, final P params) {
		final LogProcessContext logProcessContext = transSynchronizer.getLogProcessContext();
		
		//check parent transaction, SAGA-TCC is not support in subTransaction
		if(MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY) != null) {
			throw new RuntimeException("Saga-tcc is only allow in master transaction");
		}
		
		//save the SAGA calling request, for the SAGA handling after the master transaction
		PreSagaTccCallContent content = new PreSagaTccCallContent();
		content.setParams(params);
		content.setCallSeq(callSeq);
		logProcessContext.getLogCache().cacheLog(content);
		
		//SAGA do not return message synchronous， just return a Meaningless result
		return nullObject;
	}
	
	@Override
	public boolean preLogProcess(LogProcessContext ctx, Content currentContent) {
		
		// call the remote sagaTry method when transactionStatus is unknown.
		// if transactionStatus is not null, it means the method below has already called
		if(ctx.getMasterTransactionStatusVotter().getTransactionStatus() == null) {
			PreSagaTccCallContent sagaLog = (PreSagaTccCallContent) currentContent;
			EasyTransRequest<?, ?> params = sagaLog.getParams();
			BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
			try {
				rpcClient.call(businessIdentifer.appId(), businessIdentifer.busCode(), sagaLog.getCallSeq(), TRY_METHOD_NAME, params,ctx);
			} catch (Exception e) {
				LOG.warn("saga try call failed" + sagaLog,e);
				//execute failed, vote to roll back
				ctx.getMasterTransactionStatusVotter().veto();
			}
		}
		return true;
	}

	@Override
	public boolean logProcess(LogProcessContext ctx, Content currentContent) {
		if(currentContent instanceof PreSagaTccCallContent){
			PreSagaTccCallContent preCallContent = (PreSagaTccCallContent) currentContent;
			//register DemiLogEvent
			ctx.getDemiLogManager().registerSemiLogEventListener(preCallContent, this);
		}
		return true;
	}

	@Override
	public boolean onMatch(LogProcessContext logCtx, Content leftContent, Content rightContent) {
		return true;// do nothig
	}

	@Override
	public boolean onDismatch(LogProcessContext logCtx, Content leftContent) {
		PreSagaTccCallContent preCallContent = (PreSagaTccCallContent) leftContent;
		EasyTransRequest<?,?> params = preCallContent.getParams();
		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
		if(logCtx.getFinalMasterTransStatus() == null){
			LOG.info("final trans status unknown,process later." + logCtx.getLogCollection());
			return false;//unknown,process later
		}else if(logCtx.getFinalMasterTransStatus()){
			//commit
			//execute confirm and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCallContent.getCallSeq(), CONFIRM_METHOD_NAME, preCallContent.getParams(),logCtx);
			SagaTccCallConfirmedContent tccCallConfirmedContent = new SagaTccCallConfirmedContent();
			tccCallConfirmedContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(tccCallConfirmedContent);
			return true;
		}else{
			//roll back
			//execute cancel and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCallContent.getCallSeq(), CANCEL_METHOD_NAME, preCallContent.getParams(),logCtx);
			SagaTccCallCancelledContent tccCallCanceledContent = new SagaTccCallCancelledContent();
			tccCallCanceledContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(tccCallCanceledContent);
			return true;
		}
	}

	


}
