package com.yiqiniu.easytrans.queue.impl.ons;

import java.util.Map;
import java.util.Properties;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublishResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class OnsEasyTransMsgPublisherImpl implements EasyTransMsgPublisher {

	public static final String MESSAGE_LEN = "mLen";
	public static final String HEADER_LEN = "hLen";
	private Producer producer;
	private ObjectSerializer serializer;
	
	public OnsEasyTransMsgPublisherImpl(Map<Object,Object> propertyMap,ObjectSerializer serializer){
		this.serializer = serializer;
		Properties properties = new Properties();
		properties.putAll(propertyMap);
		producer = ONSFactory.createProducer(properties);
		producer.start();
	}

	
	@Override
	public EasyTransMsgPublishResult publish(String topic, String tag, String key, Map<String,Object> header, byte[] msgByte) {
		byte[] headerBytes = serializer.serialization(header);
		int headerBytesLen = headerBytes.length;
		int messageByteLen = msgByte.length;
		byte[] combine = new byte[headerBytesLen + messageByteLen];
		System.arraycopy(headerBytes, 0, combine, 0, headerBytes.length);
		System.arraycopy(msgByte, 0, combine, headerBytes.length , msgByte.length);
		
		Message message =  new Message(topic, tag, key, combine);
		Properties properties = new Properties();
		properties.put(HEADER_LEN, headerBytesLen);
		properties.put(MESSAGE_LEN, messageByteLen);
		message.setUserProperties(properties);
		SendResult send = producer.send(message);
		EasyTransMsgPublishResult easyTransMsgPublishResult = new EasyTransMsgPublishResult();
		easyTransMsgPublishResult.setTopic(send.getTopic());
		easyTransMsgPublishResult.setMessageId(send.getMessageId());
		return easyTransMsgPublishResult;
	}

}
