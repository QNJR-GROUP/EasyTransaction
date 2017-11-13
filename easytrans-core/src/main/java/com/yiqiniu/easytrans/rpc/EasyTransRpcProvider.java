package com.yiqiniu.easytrans.rpc;

import java.util.List;
import java.util.Map;

import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

public interface EasyTransRpcProvider {
    /**
     * start the service list offered
     * @param businessInterface the service interface
     * @param businessList detailServiceImpl
     */
    void startService(Class<?> businessInterface,Map<BusinessIdentifer,RpcBusinessProvider<?>> businessList);
    
    /**
     * add EasyTransFilter to RPC filters
     * @param filters ordered filter
     */
    void addEasyTransFilter(List<EasyTransFilter> filters);
}
