package com.yiqiniu.easytrans.protocol;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.EasyTransExecutor;



public abstract class AbstractEasyTransRequest<R extends Serializable,E extends EasyTransExecutor> implements EasyTransRequest<R,E>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
