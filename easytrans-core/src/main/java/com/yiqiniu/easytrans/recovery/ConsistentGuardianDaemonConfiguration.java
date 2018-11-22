package com.yiqiniu.easytrans.recovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.core.ConsistentGuardian;
import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.master.EasyTransMasterSelector;

/** 
* @author xudeyou 
*/
@Configuration
@EnableConfigurationProperties(ConsistentGuardianDaemonProperties.class)
public class ConsistentGuardianDaemonConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnProperty(name="easytrans.recovery.enabled",matchIfMissing = true)
	public ConsistentGuardianDaemon consistentGuardianDaemon(ConsistentGuardianDaemonProperties properties,TransactionLogReader logReader, ConsistentGuardian guardian,
			EasyTransMasterSelector master){
		return new ConsistentGuardianDaemon(logReader, guardian, master, properties.getExecuteInterval(), properties.getPageSize(), properties.getDelay());
	}
	
}
