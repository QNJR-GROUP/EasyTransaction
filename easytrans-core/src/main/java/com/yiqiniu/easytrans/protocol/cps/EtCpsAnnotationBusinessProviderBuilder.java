package com.yiqiniu.easytrans.protocol.cps;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.yiqiniu.easytrans.protocol.AnnotationBusinessProviderBuilder;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.RequestClassAware;

public class EtCpsAnnotationBusinessProviderBuilder extends AnnotationBusinessProviderBuilder {

    @Override
    public BusinessProvider<?> create(Annotation ann, final Object proxyObj, Method targetMethod, Class<?> requestClass, String beanName) {
        
        EtCps prividerAnn = (EtCps) ann;

        
        Method cancel = null;
        Method[] methods = proxyObj.getClass().getMethods();
        for(Method m : methods) {
            if(m.getName().equals(prividerAnn.cancelMethod())) {
                if(cancel != null) {
                    throw new RuntimeException("not allow duplicated method name  " + prividerAnn.cancelMethod() + " in class " + proxyObj.getClass());
                } else {
                    cancel = m;
                }
            }
        }

        if(cancel == null) {
            throw new IllegalArgumentException("can not find specifed confirm/cancel method: " + prividerAnn);
        }
        
        if(prividerAnn.cfgClass() != NullEasyTransRequest.class) {
            requestClass = prividerAnn.cfgClass();
        }
        
        final Method finalCancel = cancel;
        final Class<?> finalRequestClass = requestClass;
        
        Object cps = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {CompensableMethod.class,RequestClassAware.class}, new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                
                Object springProxiedBean = getApplicationContext().getBean(beanName);
                
                switch(method.getName()) {
                case BusinessProvider.GET_IDEMPOTENT_TYPE:
                    return prividerAnn.idempotentType();
                case RequestClassAware.GET_REQUEST_CLASS:
                    return finalRequestClass;
                case CompensableMethod.DO_COMPENSABLE_BUSINESS:
                    return targetMethod.invoke(springProxiedBean, args);
                case CompensableMethod.COMPENSATION:
                    return finalCancel.invoke(springProxiedBean, args);
                default:
                    throw new RuntimeException("not recognized method!" + method);
                }
                
            }
        });
        
        return (BusinessProvider<?>) cps;
    }

    @Override
    public Class<? extends Annotation> getTargetAnnotation() {
        return EtCps.class;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EasyTransRequest<?, ?>> getActualConfigClass(Annotation ann, Class<?> requestClass) {
        
        EtCps prividerAnn = (EtCps) ann;
        
        if(prividerAnn.cfgClass() != NullEasyTransRequest.class) {
            return (Class<? extends EasyTransRequest<?, ?>>) prividerAnn.cfgClass();
        } else {
            return (Class<? extends EasyTransRequest<?, ?>>) requestClass;
        }
        
    }

}
