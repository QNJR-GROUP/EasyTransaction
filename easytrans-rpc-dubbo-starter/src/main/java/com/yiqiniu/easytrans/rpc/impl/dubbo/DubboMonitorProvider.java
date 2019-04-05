package com.yiqiniu.easytrans.rpc.impl.dubbo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson.JSON;
import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.monitor.EtMonitor;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class DubboMonitorProvider {
    
    private List<EtMonitor> etMonitorList;
    private ApplicationConfig applicationConfig; 
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private ProviderConfig providerConfig;
    private ModuleConfig moduleConfig;
    private MonitorConfig monitorConfig;
    private DubboServiceCustomizationer customizationer;
    private String appId;
    
    private static Logger logger = LoggerFactory.getLogger(DubboEasyTransRpcProviderImpl.class);
    
    public DubboMonitorProvider(String appId, EasyTransFilterChainFactory filterChainFactory, Optional<ApplicationConfig> applicationConfig,
            Optional<RegistryConfig> registryConfig,Optional<ProtocolConfig> protocolConfig,Optional<ProviderConfig> providerConfig,Optional<ModuleConfig> moduleConfig,Optional<MonitorConfig> monitorConfig, Optional<DubboServiceCustomizationer> customizationer,List<EtMonitor> etMonitorList) {
        super();
        this.appId = appId;
        this.applicationConfig = applicationConfig.orElse(null);
        this.registryConfig = registryConfig.orElse(null);
        this.protocolConfig = protocolConfig.orElse(null);
        this.providerConfig = providerConfig.orElse(null);
        this.moduleConfig = moduleConfig.orElse(null);
        this.monitorConfig = monitorConfig.orElse(null);
        this.customizationer = customizationer.orElse(null);
        this.etMonitorList = etMonitorList;
    }

    @PostConstruct
    public void initHandler() throws NoSuchMethodException, SecurityException {
        
        for(EtMonitor monitor : etMonitorList) {
            
            Class<? extends EtMonitor> monitorClass = monitor.getClass();
            Class<?> markClass = ReflectUtil.getClassWithMark(monitorClass,EtMonitor.class);
            if(!markClass.isInterface()) {
                throw new RuntimeException("EtMonitor should mark in interface but not class!");
            }
            
            GenericService genericService = new GenericService() {
                
                @Override
                public Object $invoke(String method, String[] parameterTypes, Object[] args)
                        throws GenericException {
                    Method m = getMethod(markClass, method, parameterTypes);
                    if(m == null) {
                        throw new RuntimeException("can not find method !" + markClass + method + parameterTypes) ;
                    }
                    
                    try {
                        if(logger.isDebugEnabled()) {
                            logger.debug("Monitor method called! {} {} {}",markClass.getSimpleName(),method,args);
                        }
                        
                        //to JSON string to avoid serialize problem 
                        return JSON.toJSON(m.invoke(monitor, args));
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }

                private Method getMethod(Class<?> interfaceClass, String methodName, String[] parameterTypes) {
                    Method[] methods = interfaceClass.getMethods();
                    for(Method m : methods) {
                        if(m.getName().equals(methodName)) {
                            String[] methodParameters = Arrays.stream(m.getParameterTypes()).map(t->t.getName()).toArray(String[]::new);
                            if(Arrays.equals(methodParameters, parameterTypes)) {
                                return m;
                            }
                        }
                    }
                    return null;
                }
            };
            
            ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
            service.setInterface(markClass);
            service.setGroup(appId + "-" + markClass.getSimpleName());
            service.setVersion("1.0.0");
            service.setRef(genericService);
            service.setCluster("failfast");
            
            if(applicationConfig != null) {
                service.setApplication(applicationConfig);
            }
            
            if(registryConfig != null) {
                service.setRegistry(registryConfig);
            }
            
            if(protocolConfig != null) {
                service.setProtocol(protocolConfig);
            }
            
            if(monitorConfig != null) {
                service.setMonitor(monitorConfig);
            }
            
            if(moduleConfig != null) {
                service.setModule(moduleConfig);
            }
            
            if(providerConfig != null) {
                service.setProvider(providerConfig);
            }
            
            if(customizationer != null) {
                customizationer.customDubboService(null,service);
            }
            
            service.export();
        }
        
    }
}
