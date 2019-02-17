package com.yiqiniu.easytrans.protocol.autocps;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.yiqiniu.easytrans.protocol.AnnotationBusinessProviderBuilder;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.RequestClassAware;

public class EtAutoCpsAnnotationBusinessProviderBuilder extends AnnotationBusinessProviderBuilder {

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public BusinessProvider<?> create(Annotation ann, final Object proxyObj, Method targetMethod, Class<?> requestClass, String beanName) {
        
        EtAutoCps tccAnn = (EtAutoCps) ann;

        if(tccAnn.cfgClass() != NullEasyTransRequest.class) {
            requestClass = tccAnn.cfgClass();
        }
        
        final Class<?> finalRequestClass = requestClass;
        
        final AutoCpsMethod acm = new AbstractAutoCpsMethod() {

            @Override
            public int getIdempotentType() {
                return tccAnn.idempotentType();
            }

            @Override
            protected Serializable doBusiness(AutoCpsMethodRequest param) {
                
                Object springProxiedBean = getApplicationContext().getBean(beanName);

                try {
                    return (Serializable) targetMethod.invoke(springProxiedBean, param);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new RuntimeException("Auto cps business call exception!",e);
                }
            }
        };
        
        
        Object acps = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {AutoCpsMethod.class,RequestClassAware.class}, new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                
                
                switch(method.getName()) {
                case BusinessProvider.GET_IDEMPOTENT_TYPE:
                    return acm.getIdempotentType();
                case RequestClassAware.GET_REQUEST_CLASS:
                    return finalRequestClass;
                case AutoCpsMethod.DO_AUTO_CPS_BUSINESS:
                    return acm.doAutoCpsBusiness((AutoCpsMethodRequest) args[0]);
                case AutoCpsMethod.DO_AUTO_CPS_COMMIT:
                    acm.doAutoCpsCommit((AutoCpsMethodRequest) args[0]);
                    return null;
                case AutoCpsMethod.DO_AUTO_CPS_ROLLBACK:
                    acm.doAutoCpsRollback((AutoCpsMethodRequest) args[0]);
                    return null;
                default:
                    throw new RuntimeException("not recognized method!" + method);
                }
                
            }
        });
        
        return (BusinessProvider<?>) acps;
    }

    @Override
    public Class<? extends Annotation> getTargetAnnotation() {
        return EtAutoCps.class;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EasyTransRequest<?, ?>> getActualConfigClass(Annotation ann, Class<?> requestClass) {
        
        EtAutoCps prividerAnn = (EtAutoCps) ann;
        
        if(prividerAnn.cfgClass() != NullEasyTransRequest.class) {
            return (Class<? extends EasyTransRequest<?, ?>>) prividerAnn.cfgClass();
        } else {
            return (Class<? extends EasyTransRequest<?, ?>>) requestClass;
        }
        
    }

}
