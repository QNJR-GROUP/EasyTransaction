package com.yiqiniu.easytrans.queue.impl.ons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgConsumer;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.queue.ons.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(OnsQueueProperties.class)
public class OnsQueueConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMsgConsumer.class)
	public EasyTransMsgConsumer onsEasyTransMsgConsumerImpl(OnsQueueProperties properties, ObjectSerializer serializer){
		return new OnsEasyTransMsgConsumerImpl(properties.getConsumer(),serializer);
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMsgPublisher.class)
	public EasyTransMsgPublisher easyTransMsgPublisher(OnsQueueProperties properties, ObjectSerializer serializer){
		return new OnsEasyTransMsgPublisherImpl(properties.getPublisher(), serializer);
	}
	
}
