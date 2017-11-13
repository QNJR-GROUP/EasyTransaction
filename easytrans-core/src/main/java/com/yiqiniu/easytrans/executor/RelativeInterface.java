package com.yiqiniu.easytrans.executor;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * the relative business implement interface
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface RelativeInterface{
	Class<?> value();
}
