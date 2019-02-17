package com.yiqiniu.easytrans.protocol;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.yiqiniu.easytrans.executor.EasyTransExecutor;

public abstract class AnnotationBusinessProviderBuilder implements ApplicationContextAware {
    
    private ApplicationContext ctx;
    
    protected ApplicationContext getApplicationContext() {
        return ctx;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
    
    
    public abstract Class<? extends Annotation> getTargetAnnotation();
    
    public abstract BusinessProvider<?> create(Annotation ann, Object proxyObj, Method targetMethod, Class<?> requestClass, String beanName);
    
    public abstract Class<? extends EasyTransRequest<?, ?>> getActualConfigClass(Annotation ann, Class<?> requestClass);
    
    @SuppressWarnings("rawtypes")
    public static class NullEasyTransRequest<R extends Serializable,E extends EasyTransExecutor> implements EasyTransRequest{
        private static final long serialVersionUID = 1L;
        
    }
}
