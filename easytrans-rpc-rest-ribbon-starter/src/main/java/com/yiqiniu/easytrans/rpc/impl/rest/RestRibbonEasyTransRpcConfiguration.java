package com.yiqiniu.easytrans.rpc.impl.rest;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClientSpecification;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProvider;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.rpc.rest-ribbon.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(RestRibbonEasyTransRpcProperties.class)
public class RestRibbonEasyTransRpcConfiguration {
	
	@Bean
	@LoadBalanced
	public RestTemplate restTempalte(){
		return new RestTemplate();
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransRpcConsumer.class)
	public RestRibbonEasyTransRpcConsumerImpl restRibbonEasyTransRpcConsumerImpl(RestRibbonEasyTransRpcProperties properties, ObjectSerializer serializer, ApplicationContext ctx, RestTemplate restTemplate, List<RibbonClientSpecification> configurations){
		return new RestRibbonEasyTransRpcConsumerImpl(properties, serializer, ctx, configurations);
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransRpcProvider.class)
	public RestRibbonEasyTransRpcProviderImpl restRibbonEasyTransRpcProviderImpl(EasyTransFilterChainFactory filterChainFactory, ObjectSerializer serializer){
		return new RestRibbonEasyTransRpcProviderImpl(filterChainFactory, serializer);
	}
	
}
