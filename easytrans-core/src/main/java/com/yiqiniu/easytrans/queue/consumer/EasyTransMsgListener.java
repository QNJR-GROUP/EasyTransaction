package com.yiqiniu.easytrans.queue.consumer;

import java.util.Map;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public interface EasyTransMsgListener {

	
    public EasyTransConsumeAction consume(Map<String,Object> header,EasyTransRequest<?, ?> message);
}