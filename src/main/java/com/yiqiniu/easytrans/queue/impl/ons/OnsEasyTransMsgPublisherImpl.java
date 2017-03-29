package com.yiqiniu.easytrans.queue.impl.ons;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.SendResult;
import com.yiqiniu.easytrans.config.EasyTransConifg;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublishResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;

public class OnsEasyTransMsgPublisherImpl implements EasyTransMsgPublisher {

	private Producer producer;
	
	@Resource
	private EasyTransConifg config;
	
	@PostConstruct
	private void init(){
		Properties properties = new Properties();
		properties.put(PropertyKeyConst.ONSAddr, config.getExtendConfig("easytrans.queue.ons.addr")); // 阿里云身份验证，在阿里云服务器管理控制台创建
		properties.put(PropertyKeyConst.AccessKey, config.getExtendConfig("easytrans.queue.ons.producer.key")); // 阿里云身份验证，在阿里云服务器管理控制台创建
		properties.put(PropertyKeyConst.SecretKey, config.getExtendConfig("easytrans.queue.ons.producer.secrect"));// 此处以公有云生产环境为例
		properties.put(PropertyKeyConst.ProducerId, config.getExtendConfig("easytrans.queue.ons.producer.name"));
		producer = ONSFactory.createProducer(properties);
		producer.start();
	}
	
	@Override
	public EasyTransMsgPublishResult publish(String topic, String tag, String key, byte[] msgByte) {
		Message message =  new Message(topic, tag, key, msgByte);
		SendResult send = producer.send(message);
		EasyTransMsgPublishResult easyTransMsgPublishResult = new EasyTransMsgPublishResult();
		easyTransMsgPublishResult.setTopic(send.getTopic());
		easyTransMsgPublishResult.setMessageId(send.getMessageId());
		return easyTransMsgPublishResult;
	}

}
