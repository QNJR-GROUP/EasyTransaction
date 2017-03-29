package com.yiqiniu.easytrans.filter;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface EasyTransFilterChain {
	
	String MESSAGE_BUSINESS_FLAG = "@MSG";
	
	String getAppId();
	
    String getBusCode();
    
    String getInnerMethodName();
    
    void addFilter(EasyTransFilter filter);

    /**
     * invoke filter chain
     * 
     * @param invocation
     * @return result
     */
    EasyTransResult invokeFilterChain(EasyTransRequest<?, ?> request);
    
    /**
     * 
     * @param key
     * @param resource origin resource
     * @return
     */
    Object bindResource(String key,Object resource);
    
    /**
     * get bound resource
     * @param key
     * @return
     */
    <T> T getResource(String key);
}