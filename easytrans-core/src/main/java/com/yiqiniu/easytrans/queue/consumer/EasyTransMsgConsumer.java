package com.yiqiniu.easytrans.queue.consumer;

import java.util.Collection;


public interface EasyTransMsgConsumer {

    /**
     * subscribe topic,override the previous subscription
     */
    void subscribe(String topic, Collection<String> tag,EasyTransMsgListener listener);
    
    void start();
	
    String getConsumerId();
}