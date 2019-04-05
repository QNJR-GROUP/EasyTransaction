package com.yiqiniu.easytrans.rpc.impl.rest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import com.yiqiniu.easytrans.monitor.EtMonitor;
import com.yiqiniu.easytrans.monitor.MonitorConsumerFactory;

public class RestRibbonMonitorConsumerFactory implements MonitorConsumerFactory {
    
    private RestRibbonEasyTransRpcConsumerImpl consumer;
    private ConcurrentHashMap<String, EtMonitor> mapMonitor = new ConcurrentHashMap<>();
    
    public RestRibbonMonitorConsumerFactory(RestRibbonEasyTransRpcConsumerImpl consumer) {
        super();
        this.consumer = consumer;
    }

    @SuppressWarnings("unchecked")
    public <T extends EtMonitor> T getRemoteProxy(String appId, Class<T> monitorInterface) {
        
        return (T) mapMonitor.computeIfAbsent(appId + "|" + monitorInterface.getSimpleName() , k->{
            return (EtMonitor)Proxy.newProxyInstance(monitorInterface.getClassLoader(), new Class[] {monitorInterface}, new InvocationHandler() {
                
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return consumer.sendMonitorRequest(appId, monitorInterface, method, args);
                }
            });
        });
        
    }
    
    
}
