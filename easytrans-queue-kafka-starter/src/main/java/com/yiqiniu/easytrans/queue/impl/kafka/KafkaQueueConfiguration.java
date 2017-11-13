package com.yiqiniu.easytrans.queue.impl.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.queue.kafka.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(KafkaQueueProperties.class)
public class KafkaQueueConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;

	
}
