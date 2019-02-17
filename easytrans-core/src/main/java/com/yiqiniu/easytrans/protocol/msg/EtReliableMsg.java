package com.yiqiniu.easytrans.protocol.msg;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.yiqiniu.easytrans.protocol.AnnotationBusinessProviderBuilder.NullEasyTransRequest;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

/**
 * place in method of the bean that managed by spring 
 * this method will do the TCC try
 * confirmMethod and cancelMethod should in the same class
 * @author xudeyou
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SuppressWarnings("rawtypes")
public @interface EtReliableMsg {

    /**
     * BusinessProvider.IDENPOTENT_TYPE_FRAMEWORK
     * BusinessProvider.IDENPOTENT_TYPE_BUSINESS
     * @return
     */
    int idempotentType();
    
    
    /**
     * 当标注的方法的入参并非继承自EasyTransRequest时，需要用本字段指定继承自该类的类
     * @return
     */
    Class<? extends EasyTransRequest> cfgClass() default NullEasyTransRequest.class; 
}
