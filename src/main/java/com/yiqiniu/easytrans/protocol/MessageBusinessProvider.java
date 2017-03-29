package com.yiqiniu.easytrans.protocol;

import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;


public interface MessageBusinessProvider<P extends EasyTransRequest<?, ?>> extends BusinessProvider<P> {
	  public EasyTransConsumeAction consume(EasyTransRequest<?, ?> request);
}
