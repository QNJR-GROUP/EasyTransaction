package com.yiqiniu.easytrans.filter;

import java.util.Map;

import org.springframework.core.annotation.Order;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;

@Order(0)
public class MetaDataFilter implements EasyTransFilter {
	
	private static ThreadLocal<Map<String, Object>> threadLocalHeader = new ThreadLocal<>();

	@Override
	public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String, Object> header,
			EasyTransRequest<?, ?> request) {
		
		EasyTransResult result = null;
		try {
			threadLocalHeader.set(header);
			result = filterChain.invokeFilterChain(header, request);
		} finally {
			threadLocalHeader.remove();
		}
		return result;
	}
	
	public static Map<String,Object> getMetaDataMap(){
		return threadLocalHeader.get();
	}
	
	@SuppressWarnings("unchecked")
	public static <R>  R getMetaData(String key){
		Map<String, Object> map = threadLocalHeader.get();
		return (R)map.get(key);
	}

}
