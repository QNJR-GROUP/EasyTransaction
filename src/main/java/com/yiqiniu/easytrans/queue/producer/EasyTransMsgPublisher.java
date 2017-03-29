package com.yiqiniu.easytrans.queue.producer;

public interface EasyTransMsgPublisher {

    /**
     * send message directory<br/>
     *
     * @param message
     * @return message sent info
     */
	EasyTransMsgPublishResult publish(String topic, String tag, String key, byte[] msgByte);
}
