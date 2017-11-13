package com.yiqiniu.easytrans.log.impl.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.log.kafka.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(KafkaTransactionLogProperties.class)
public class KafkaTransactionLogConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	
	
}
