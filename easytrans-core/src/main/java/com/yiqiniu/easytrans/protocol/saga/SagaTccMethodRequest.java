package com.yiqiniu.easytrans.protocol.saga;

import com.yiqiniu.easytrans.executor.SagaTccMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.SerializableVoid;

public interface SagaTccMethodRequest extends EasyTransRequest<SerializableVoid,SagaTccMethodExecutor> {
	
}
