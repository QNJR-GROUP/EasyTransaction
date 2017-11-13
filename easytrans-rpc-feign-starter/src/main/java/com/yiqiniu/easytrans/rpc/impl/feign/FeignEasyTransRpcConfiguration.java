package com.yiqiniu.easytrans.rpc.impl.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.rpc.feign.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(FeignEasyTransRpcProperties.class)
public class FeignEasyTransRpcConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
}
