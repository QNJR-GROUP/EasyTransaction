package com.yiqiniu.easytrans.protocol;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.EasyTransExecutor;

/**
 * base interface for soft transaction parameters<br/>
 */
public interface EasyTransRequest<R extends Serializable,E extends EasyTransExecutor> extends Serializable{
	
}
