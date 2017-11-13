package com.yiqiniu.easytrans.filter;

import java.util.List;

public interface EasyTransFilterChainFactory {
	
	/**
	 * get default filter chain<br/>
	 * RPC,Queue Implement should wrap the user business code to a filter,and then add to the chain end<br/>
	 * so before execute the user business code,we filter the request
	 * @param appId
	 * @param busCode
	 * @param innerMethod
	 * @return
	 */
	EasyTransFilterChain getDefaultFilterChain(String appId,String busCode,String innerMethod);
	
	EasyTransFilterChain getFilterChainByFilters(String appId,String busCode,String innerMethod,List<EasyTransFilter> filters);
	
	List<EasyTransFilter> getDefaultFilters();
}
