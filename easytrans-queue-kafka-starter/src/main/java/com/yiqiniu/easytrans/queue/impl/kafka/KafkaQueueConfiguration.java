package com.yiqiniu.easytrans.queue.impl.kafka;

import java.util.Collection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgConsumer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgListener;
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
	public EasyTransMsgConsumer easyTransMsgConsumerImpl(KafkaQueueProperties properties, ObjectSerializer serializer, KafkaEasyTransMsgPublisherImpl kafkaPublisher){
		if(properties.getConsumerCfg() != null) {
			return new KafkaEasyTransMsgConsumerImpl(properties.getConsumerCfg(),serializer, kafkaPublisher);
		} else {
			return new EasyTransMsgConsumer(){

				@Override
				public void subscribe(String topic, Collection<String> tag, EasyTransMsgListener listener) {
					throw new RuntimeException("Consumer not configured!");
				}

				@Override
				public void start() {
					throw new RuntimeException("Consumer not configured!");
				}

				@Override
				public String getConsumerId() {
					throw new RuntimeException("Consumer not configured!");
				}
			};
		}
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMsgPublisher.class)
	public EasyTransMsgPublisher easyTransMsgPublisher(KafkaQueueProperties properties, ObjectSerializer serializer){
		return new KafkaEasyTransMsgPublisherImpl(properties.getProduerCfg(), serializer);
	}
	
}
