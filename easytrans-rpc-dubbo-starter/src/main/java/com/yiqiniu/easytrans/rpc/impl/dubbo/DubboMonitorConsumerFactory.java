package com.yiqiniu.easytrans.rpc.impl.dubbo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.yiqiniu.easytrans.monitor.EtMonitor;
import com.yiqiniu.easytrans.monitor.MonitorConsumerFactory;

public class DubboMonitorConsumerFactory implements MonitorConsumerFactory {
    
    private ApplicationConfig applicationConfig; 
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private ConsumerConfig consumerConfig;
    private ModuleConfig moduleConfig;
    private MonitorConfig monitorConfig;
    private DubboReferanceCustomizationer customizationer;
    
    public DubboMonitorConsumerFactory(Optional<ApplicationConfig> applicationConfig, Optional<RegistryConfig> registryConfig,Optional<ProtocolConfig> protocolConfig,Optional<ConsumerConfig> consumerConfig,Optional<ModuleConfig> moduleConfig,Optional<MonitorConfig> monitorConfig, Optional<DubboReferanceCustomizationer> customizationer) {
        super();
        this.applicationConfig = applicationConfig.orElse(null);
        this.registryConfig = registryConfig.orElse(null);
        this.protocolConfig = protocolConfig.orElse(null);
        this.customizationer = customizationer.orElse(null);
        this.consumerConfig = consumerConfig.orElse(null);
        this.moduleConfig = moduleConfig.orElse(null);
        this.monitorConfig = monitorConfig.orElse(null);
    }

    private ConcurrentHashMap<String, EtMonitor> mapMonitor = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends EtMonitor> T getRemoteProxy(String appId, Class<T> monitorInterface) {
        return (T) mapMonitor.computeIfAbsent(getKey(appId, monitorInterface), k -> {
            return generateProxy(appId, monitorInterface);
        });

    }

    private <T extends EtMonitor> EtMonitor generateProxy(String appId, Class<T> monitorInterface) {
        return (EtMonitor) Proxy.newProxyInstance(monitorInterface.getClassLoader(), new Class[] { monitorInterface }, new InvocationHandler() {
            
            private GenericService service = generateService(appId, monitorInterface);
            private ConcurrentHashMap<Method, String[]> mapParameterCLassString = new ConcurrentHashMap<>();
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                
                String[] paramTypeStr = mapParameterCLassString.computeIfAbsent(method, m->{
                    return Arrays.stream(m.getParameterTypes()).map(clazz->clazz.getName()).toArray(String[]::new);
                });
                
                return service.$invoke(method.getName(), paramTypeStr, args);
            }
        });
    }

    private <T extends EtMonitor> String getKey(String appId, Class<T> monitorInterface) {
        return appId + "|" + monitorInterface.getSimpleName();
    }
    
    private GenericService generateService(String appId, Class<?> monitorInterface) {
        
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<GenericService>();
        referenceConfig.setInterface(monitorInterface); // 弱类型接口名 
        referenceConfig.setVersion("1.0.0"); 
        referenceConfig.setGeneric(true); // 声明为泛化接口 
        referenceConfig.setGroup(appId + "-" + monitorInterface.getSimpleName());
        referenceConfig.setCheck(false);
        
        if(applicationConfig != null) {
            referenceConfig.setApplication(applicationConfig);
        }
        
        if(registryConfig != null) {
            referenceConfig.setRegistry(registryConfig);
        }
        
        if(protocolConfig != null) {
            referenceConfig.setProtocol(protocolConfig.getName());
        }
        
        if(moduleConfig != null) {
            referenceConfig.setModule(moduleConfig);
        }
        
        if(monitorConfig != null) {
            referenceConfig.setMonitor(monitorConfig);
        }
        
        if(consumerConfig != null) {
            referenceConfig.setConsumer(consumerConfig);
        }
        
        if(customizationer != null) {
            customizationer.customDubboReferance(appId,null,referenceConfig);
        }
        
        return referenceConfig.get();
    }

}
