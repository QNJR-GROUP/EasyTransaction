package com.yiqiniu.easytrans.protocol;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

import com.yiqiniu.easytrans.util.ReflectUtil;

public class AnnotationProviderRegister extends InstantiationAwareBeanPostProcessorAdapter {

    private Map<Class<? extends Annotation>,AnnotationBusinessProviderBuilder> mapHandler;
    private ConfigurableListableBeanFactory beanFactory;
    
    public AnnotationProviderRegister(List<AnnotationBusinessProviderBuilder> listHandler, ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        mapHandler = new HashMap<>(listHandler.size() * 2);
        for(AnnotationBusinessProviderBuilder handler :listHandler) {
            mapHandler.put(handler.getTargetAnnotation(), handler);
        }
    }

    
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        if(bean instanceof AnnotationBusinessProviderBuilder) {
            return true;
        }
        
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for(Method m : targetClass.getMethods()) {
            for(Annotation a : m.getAnnotations()) {
                AnnotationBusinessProviderBuilder builder = mapHandler.get(a.annotationType());
                if(builder != null) {
                    
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if(parameterTypes.length != 1) {
                        throw new IllegalArgumentException("method should conatins only One parameter, method:" + m);
                    }
                    
                    @SuppressWarnings("unchecked")
                    Class<? extends EasyTransRequest<?, ?>> requestClass = (Class<? extends EasyTransRequest<?, ?>>) parameterTypes[0];
                    
                  //create and register in bean factory
                    BusinessProvider<?> provider = builder.create(a, bean, m, requestClass,beanName);
                    
                    Class<? extends EasyTransRequest<?, ?>> cfgClass = builder.getActualConfigClass(a, requestClass);
                    BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(cfgClass);
                    
                    if(businessIdentifer == null) {
                        throw new RuntimeException("can not find BusinessIdentifer in request class!" + requestClass);
                    }
                    
                    beanFactory.registerSingleton(businessIdentifer.appId() + "-" + businessIdentifer.busCode() +"-provider", provider);
                }
            }
        }
        return true;
    }
}
