package com.yiqiniu.easytrans.protocol;

import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;


public interface MessageBusinessProvider<P extends EasyTransRequest<?, ?>> extends BusinessProvider<P> {
	//indicate isSynchronousMethod to be true,to save the return value
	@ExecuteOrder(doNotExecuteAfter = {}, ifNotExecutedReturnDirectly = {}, isSynchronousMethod=true)  
	public EasyTransConsumeAction consume(EasyTransRequest<?, ?> request);
}
