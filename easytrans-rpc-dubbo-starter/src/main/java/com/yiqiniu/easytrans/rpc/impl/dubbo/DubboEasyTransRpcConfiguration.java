package com.yiqiniu.easytrans.rpc.impl.dubbo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProvider;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.rpc.dubbo.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(DubboEasyTransRpcProperties.class)
public class DubboEasyTransRpcConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnMissingBean(EasyTransRpcConsumer.class)
	public DubboEasyTransRpcConsumerImpl dubboEasyTransRpcConsumerImpl(DubboEasyTransRpcProperties properties){
		return new DubboEasyTransRpcConsumerImpl(applicationName, properties.getDubboZkUrl());
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransRpcProvider.class)
	public DubboEasyTransRpcProviderImpl onsEasyTransMsgPublisherImpl(DubboEasyTransRpcProperties properties,EasyTransFilterChainFactory filterChainFactory){
		return new DubboEasyTransRpcProviderImpl(filterChainFactory, applicationName, properties.getDubboZkUrl(), properties.getDubboDefaultTimeout());
	}
	
}
