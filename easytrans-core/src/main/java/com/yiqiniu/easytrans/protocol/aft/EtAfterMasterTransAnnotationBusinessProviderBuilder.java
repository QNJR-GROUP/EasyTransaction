package com.yiqiniu.easytrans.protocol.aft;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.yiqiniu.easytrans.protocol.AnnotationBusinessProviderBuilder;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.RequestClassAware;

public class EtAfterMasterTransAnnotationBusinessProviderBuilder extends AnnotationBusinessProviderBuilder {

    @Override
    public BusinessProvider<?> create(Annotation ann, final Object proxyObj, Method targetMethod, Class<?> requestClass, String beanName) {
        
        EtAfterMasterTrans prividerAnn = (EtAfterMasterTrans) ann;
        final Class<?> finalRequestClass = getActualConfigClass(prividerAnn,requestClass);
        
        Object cps = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {AfterMasterTransMethod.class,RequestClassAware.class}, new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                
                Object springProxiedBean = getApplicationContext().getBean(beanName);
                
                switch(method.getName()) {
                case BusinessProvider.GET_IDEMPOTENT_TYPE:
                    return prividerAnn.idempotentType();
                case RequestClassAware.GET_REQUEST_CLASS:
                    return finalRequestClass;
                case AfterMasterTransMethod.AFTER_TRANSACTION:
                    return targetMethod.invoke(springProxiedBean, args);
                default:
                    throw new RuntimeException("not recognized method!" + method);
                }
                
            }
        });
        
        return (BusinessProvider<?>) cps;
    }

    @Override
    public Class<? extends Annotation> getTargetAnnotation() {
        return EtAfterMasterTrans.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EasyTransRequest<?, ?>> getActualConfigClass(Annotation ann, Class<?> requestClass) {
        
        EtAfterMasterTrans prividerAnn = (EtAfterMasterTrans) ann;
        
        if(prividerAnn.cfgClass() != NullEasyTransRequest.class) {
            return (Class<? extends EasyTransRequest<?, ?>>) prividerAnn.cfgClass();
        } else {
            return (Class<? extends EasyTransRequest<?, ?>>) requestClass;
        }
        
    }



}
