package com.yiqiniu.easytrans.queue.consumer;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public interface EasyTransMsgListener {

	
    public EasyTransConsumeAction consume(EasyTransRequest<?, ?> message);
}