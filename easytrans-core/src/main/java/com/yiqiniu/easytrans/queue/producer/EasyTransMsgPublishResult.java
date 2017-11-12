package com.yiqiniu.easytrans.queue.producer;

public class EasyTransMsgPublishResult {
	
    private String messageId;
    private String topic;


    public String getMessageId() {
        return messageId;
    }


    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return "SendResult[topic=" + topic + ", messageId=" + messageId + ']';
    }
}
