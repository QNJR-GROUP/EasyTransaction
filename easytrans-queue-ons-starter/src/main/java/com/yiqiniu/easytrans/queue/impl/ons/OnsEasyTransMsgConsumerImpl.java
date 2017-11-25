package com.yiqiniu.easytrans.queue.impl.ons;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgConsumer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgListener;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class OnsEasyTransMsgConsumerImpl implements EasyTransMsgConsumer {

	private Consumer consumer;
	private Properties properties;
	
	private ObjectSerializer serializer;
	
	public OnsEasyTransMsgConsumerImpl(Map<Object,Object> propertyMap,ObjectSerializer serializer){
	    properties = new Properties();
		properties.putAll(propertyMap);
		consumer = ONSFactory.createConsumer(properties);
		this.serializer = serializer;
		consumer.start();
	}
	
	
	private String getAliTagsString(Collection<String> topicSubs) {
		StringBuilder sb = new StringBuilder();
		for(String s:topicSubs){
			sb.append(s);
			sb.append("||");
		}
		return sb.substring(0, sb.length() - 2);
	}
	
	
	@Override
	public void subscribe(String topic, Collection<String> tag,
			final EasyTransMsgListener listener) {
		
		consumer.subscribe(topic, getAliTagsString(tag), new MessageListener() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Action consume(Message message, ConsumeContext context) {
				Map userProperties = message.getUserProperties();
				int headerLen = Integer.parseInt(userProperties.get(OnsEasyTransMsgPublisherImpl.HEADER_LEN).toString());
//				int messageLen = Integer.parseInt(userProperties.get(OnsEasyTransMsgPublisherImpl.MESSAGE_LEN).toString());
				byte[] combined = message.getBody();
				byte[] headerBytes = Arrays.copyOfRange(combined, 0, headerLen);
				byte[] messageBytes = Arrays.copyOfRange(combined, headerLen, combined.length);
				
				
				EasyTransConsumeAction consume = listener.consume((Map) serializer.deserialize(headerBytes),(EasyTransRequest<?, ?>) serializer.deserialize(messageBytes));
				return Action.valueOf(consume.name());
			}});
	}

	@Override
	public String getConsumerId() {
		return properties.getProperty(PropertyKeyConst.ConsumerId);
	}


	@Override
	public void start() {
		//DO NOTING
	}



	

}
