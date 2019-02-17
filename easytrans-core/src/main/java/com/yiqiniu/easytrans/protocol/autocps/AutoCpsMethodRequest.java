package com.yiqiniu.easytrans.protocol.autocps;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.AutoCpsMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface AutoCpsMethodRequest<R extends Serializable> extends EasyTransRequest<R,AutoCpsMethodExecutor> {
	
}
