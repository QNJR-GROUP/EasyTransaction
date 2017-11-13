package com.yiqiniu.easytrans.protocol;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *	define the execute order of methods
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface ExecuteOrder {
	
	/**
	 * do not execute method after one of the methods in the list called
	 * typical used in TCC, if cancel-method executed,then latter try-method should not be execute
	 */
	String[] doNotExecuteAfter();
	
	/**
	 * if the one of the methods executed,then return directly without execute business implement<br/>
	 * typical used in CompensableBusiness, if doCompensableBusiness did not work,<br>
	 * then call doCompensation should not execute business codes,just return directly
	 */
	String[] ifNotExecutedReturnDirectly();
	
	/**
	 * whether is the synchronizer method for business<br/>
	 * For example,TCC's Try method is synchronizer method 
	 */
	boolean isSynchronousMethod() default false;
}
