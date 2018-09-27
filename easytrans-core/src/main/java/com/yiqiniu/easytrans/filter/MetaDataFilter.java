package com.yiqiniu.easytrans.filter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;

@Order(0)
public class MetaDataFilter implements EasyTransFilter {
	
	private static ThreadLocal<Map<String, Object>> threadLocalHeader = new ThreadLocal<>();
	private ListableProviderFactory providerFactory;
	
	public MetaDataFilter(ListableProviderFactory providerFactory){
		this.providerFactory = providerFactory;
	}

	@Override
	public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String, Object> header,
			EasyTransRequest<?, ?> request) {
		
		EasyTransResult result = null;
		try {
			addParentTrxStatusToHeader(header,filterChain,request);
			threadLocalHeader.set(header);
			result = filterChain.invokeFilterChain(header, request);
		} finally {
			threadLocalHeader.remove();
		}
		return result;
	}
	
	private void addParentTrxStatusToHeader(Map<String, Object> header, EasyTransFilterChain filterChain ,EasyTransRequest<?, ?> request) {
		int transactionStatus = getTransactionStatus(filterChain.getAppId(),filterChain.getBusCode(),filterChain.getInnerMethodName());
		header.put(EasytransConstant.CallHeadKeys.PARENT_TRANSACTION_STATUS, transactionStatus);
	}

	public static Map<String,Object> getMetaDataMap(){
		return threadLocalHeader.get();
	}
	
	@SuppressWarnings("unchecked")
	public static <R>  R getMetaData(String key){
		Map<String, Object> map = threadLocalHeader.get();
		if(map == null){
			return null;
		}
		return (R)map.get(key);
	}
	
	private ConcurrentHashMap<String, Integer> mapTransactionStatus = new ConcurrentHashMap<String, Integer>();
	private int getTransactionStatus(String appId, String busCode ,String innerMethod) {
		
		String key = getKey(appId, busCode, innerMethod);
		Integer transactionStatus = mapTransactionStatus.get(key);
		if(transactionStatus == null){
			Class<?> serviceInterface = providerFactory.getServiceInterface(appId, busCode);
			Method[] methods = serviceInterface.getMethods();
			for(Method method:methods){
				if(method.getName().equals(innerMethod)){
					MethodTransactionStatus annotation = method.getAnnotation(MethodTransactionStatus.class);
					if(annotation == null){
						throw new RuntimeException("Error provider implement,the call method shold contains a MethodTransactionStatus Annotation,method:" + method);
					}else{
						transactionStatus = annotation.value();
					}
					mapTransactionStatus.put(key, transactionStatus);
					break;
				}
			}
		}
		
		if(transactionStatus == null) {
			throw new RuntimeException("can not determine the transaction status of " + appId + " " + busCode + " " + innerMethod);
		}
		
		return transactionStatus;
	}
	
	private String getKey(String appId, String busCode ,String innerMethod){
		return appId + busCode + innerMethod;
	}

}
