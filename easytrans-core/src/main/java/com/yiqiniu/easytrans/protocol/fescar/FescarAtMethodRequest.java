package com.yiqiniu.easytrans.protocol.fescar;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.FescarAtMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface FescarAtMethodRequest<R extends Serializable> extends EasyTransRequest<R,FescarAtMethodExecutor> {
	
}
