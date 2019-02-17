package com.yiqiniu.easytrans.protocol.tcc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.yiqiniu.easytrans.protocol.AnnotationBusinessProviderBuilder;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.RequestClassAware;

public class EtTccAnnotationBusinessProviderBuilder extends AnnotationBusinessProviderBuilder {

    @Override
    public BusinessProvider<?> create(Annotation ann, final Object proxyObj, Method targetMethod, Class<?> requestClass, String beanName) {
        
        EtTcc tccAnn = (EtTcc) ann;

        
        Method confirm = null;
        Method cancel = null;
        Method[] methods = proxyObj.getClass().getMethods();
        for(Method m : methods) {
            if(m.getName().equals(tccAnn.confirmMethod())) {
                if(confirm != null) {
                    throw new RuntimeException("not allow duplicated method name " + tccAnn.confirmMethod() + " in class " + proxyObj.getClass());
                } else {
                    confirm = m;
                }
            }
            
            if(m.getName().equals(tccAnn.cancelMethod())) {
                if(cancel != null) {
                    throw new RuntimeException("not allow duplicated method name  " + tccAnn.cancelMethod() + " in class " + proxyObj.getClass());
                } else {
                    cancel = m;
                }
            }
        }

        if(confirm == null || cancel == null) {
            throw new IllegalArgumentException("can not find specifed confirm/cancel method: " + tccAnn);
        }
        
        if(tccAnn.cfgClass() != NullEasyTransRequest.class) {
            requestClass = tccAnn.cfgClass();
        }
        
        final Method finalConfirm = confirm;
        final Method finalCancel = cancel;
        final Class<?> finalRequestClass = requestClass;
        
        Object tcc = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {TccMethod.class,RequestClassAware.class}, new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                
                Object springProxiedBean = getApplicationContext().getBean(beanName);
                
                switch(method.getName()) {
                case BusinessProvider.GET_IDEMPOTENT_TYPE:
                    return tccAnn.idempotentType();
                case RequestClassAware.GET_REQUEST_CLASS:
                    return finalRequestClass;
                case TccMethod.DO_TRY:
                    return targetMethod.invoke(springProxiedBean, args);
                case TccMethod.DO_CONFIRM:
                    return finalConfirm.invoke(springProxiedBean, args);
                case TccMethod.DO_CANCEL:
                    return finalCancel.invoke(springProxiedBean, args);
                default:
                    throw new RuntimeException("not recognized method!" + method);
                }
                
            }
        });
        
        return (BusinessProvider<?>) tcc;
    }

    @Override
    public Class<? extends Annotation> getTargetAnnotation() {
        return EtTcc.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EasyTransRequest<?, ?>> getActualConfigClass(Annotation ann, Class<?> requestClass) {
        
        EtTcc prividerAnn = (EtTcc) ann;
        
        if(prividerAnn.cfgClass() != NullEasyTransRequest.class) {
            return (Class<? extends EasyTransRequest<?, ?>>) prividerAnn.cfgClass();
        } else {
            return (Class<? extends EasyTransRequest<?, ?>>) requestClass;
        }
        
    }
    
}
