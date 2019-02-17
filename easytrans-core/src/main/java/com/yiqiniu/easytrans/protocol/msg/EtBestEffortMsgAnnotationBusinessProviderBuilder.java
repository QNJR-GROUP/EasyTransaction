package com.yiqiniu.easytrans.protocol.msg;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.yiqiniu.easytrans.protocol.AnnotationBusinessProviderBuilder;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.MessageBusinessProvider;
import com.yiqiniu.easytrans.protocol.RequestClassAware;
import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;

public class EtBestEffortMsgAnnotationBusinessProviderBuilder extends AnnotationBusinessProviderBuilder {

    @Override
    public BusinessProvider<?> create(Annotation ann, final Object proxyObj, Method targetMethod, Class<?> requestClass,String beanName) {
        
        EtBestEffortMsg providerAnn = (EtBestEffortMsg) ann;

        
        if(providerAnn.cfgClass() != NullEasyTransRequest.class) {
            requestClass = providerAnn.cfgClass();
        }
        
        final Class<?> finalRequestClass = requestClass;
        
        Object msgProvider = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {BestEffortMessageHandler.class,RequestClassAware.class}, new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                
                Object springProxiedBean = getApplicationContext().getBean(beanName);
                
                switch(method.getName()) {
                case BusinessProvider.GET_IDEMPOTENT_TYPE:
                    return providerAnn.idempotentType();
                case RequestClassAware.GET_REQUEST_CLASS:
                    return finalRequestClass;
                case MessageBusinessProvider.CONSUME:
                    targetMethod.invoke(springProxiedBean, args);
                    return EasyTransConsumeAction.CommitMessage;
                default:
                    throw new RuntimeException("not recognized method!" + method);
                }
            }
        });
        
        return (BusinessProvider<?>) msgProvider;
    }

    @Override
    public Class<? extends Annotation> getTargetAnnotation() {
        return EtBestEffortMsg.class;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EasyTransRequest<?, ?>> getActualConfigClass(Annotation ann, Class<?> requestClass) {
        
        EtBestEffortMsg prividerAnn = (EtBestEffortMsg) ann;
        
        if(prividerAnn.cfgClass() != NullEasyTransRequest.class) {
            return (Class<? extends EasyTransRequest<?, ?>>) prividerAnn.cfgClass();
        } else {
            return (Class<? extends EasyTransRequest<?, ?>>) requestClass;
        }
        
    }

}
