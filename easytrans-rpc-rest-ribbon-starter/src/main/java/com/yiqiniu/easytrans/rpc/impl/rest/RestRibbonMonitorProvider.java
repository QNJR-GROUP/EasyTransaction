package com.yiqiniu.easytrans.rpc.impl.rest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.yiqiniu.easytrans.monitor.EtMonitor;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class RestRibbonMonitorProvider {
    
//    @Autowired
//    private RestRibbonEasyTransRpcProviderImpl controller;
    
    public static final String MONITOR_CONTEXT = "_monitors";
    
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    
    @Autowired
    private List<EtMonitor> etMonitorList;

    @Value("${easytrans.rpc.rest-ribbon.provider.context:" + RestRibbonEasyTransConstants.DEFAULT_URL_CONTEXT + "}")
    private String rootUrl;
    
    @PostConstruct
    public void initHandler() throws NoSuchMethodException, SecurityException {
        
        Set<Method> objectMethods = new HashSet<>(Arrays.asList(Object.class.getMethods()));
        
        for(EtMonitor monitor : etMonitorList) {
            
            Class<? extends EtMonitor> monitorClass = monitor.getClass();
            Class<?> markClass = ReflectUtil.getClassWithMark(monitorClass,EtMonitor.class);
            if(!markClass.isInterface()) {
                throw new RuntimeException("EtMonitor should mark in interface but not class!");
            }
            
            for(Method m : monitor.getClass().getMethods()) {
                
                if(objectMethods.contains(m)) {
                    continue;
                }
                
                Object proxy = Proxy.newProxyInstance(monitorClass.getClassLoader(), new Class[]{markClass}, new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        
                        if(!method.getName().equals(m.getName())) {
                            throw new RuntimeException("Illegal call! " + method.getName());
                        }
                        
                        Object result = method.invoke(monitor, args);
                        
                        return new ResponseEntity<Object>(result,HttpStatus.OK);
                    }});
                
                Method markClassMethod = markClass.getMethod(m.getName(), m.getParameterTypes());
                
                RequestMappingInfo requestMappingInfo = RequestMappingInfo
                        .paths(rootUrl + "/" + MONITOR_CONTEXT + "/" + markClass.getSimpleName() + "/" + m.getName())
                        .methods(RequestMethod.GET)
                        .produces(MediaType.APPLICATION_JSON_VALUE)
                        .build();
                
                requestMappingHandlerMapping.registerMapping(requestMappingInfo, proxy, markClassMethod);
            }
            
        }
        
    }
}
