package com.yiqiniu.easytrans.protocol.tcc;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.TccMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface TccMethodRequest<R extends Serializable> extends EasyTransRequest<R,TccMethodExecutor> {
	
}
