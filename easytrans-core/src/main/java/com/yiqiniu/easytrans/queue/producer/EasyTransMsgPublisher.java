package com.yiqiniu.easytrans.queue.producer;

import java.util.Map;

public interface EasyTransMsgPublisher {

    /**
     * send message directory<br/>
     *
     * @param message
     * @return message sent info
     */
	EasyTransMsgPublishResult publish(String topic, String tag, String key, Map<String,Object> header ,byte[] msgByte);
}
