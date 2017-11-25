package com.yiqiniu.easytrans.queue.impl.kafka;

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
@ConditionalOnProperty(name="easytrans.queue.kafka.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(KafkaQueueProperties.class)
public class KafkaQueueConfiguration {
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMsgConsumer.class)
	public KafkaEasyTransMsgConsumerImpl easyTransMsgConsumerImpl(KafkaQueueProperties properties, ObjectSerializer serializer, KafkaEasyTransMsgPublisherImpl kafkaPublisher){
		return new KafkaEasyTransMsgConsumerImpl(properties.getConsumerCfg(),serializer, kafkaPublisher);
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMsgPublisher.class)
	public KafkaEasyTransMsgPublisherImpl easyTransMsgPublisher(KafkaQueueProperties properties, ObjectSerializer serializer){
		return new KafkaEasyTransMsgPublisherImpl(properties.getProduerCfg(), serializer);
	}
	
}
