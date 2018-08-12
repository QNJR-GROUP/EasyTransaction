package com.yiqiniu.easytrans.stringcodec.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.stringcodec.StringCodec;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.stringcodec.zk.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(ZookeeperStringCodecProperties.class)
public class ZookeeperStringCodecConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnMissingBean(StringCodec.class)
	public StringCodec zooKeeperStringCodecImpl(ZookeeperStringCodecProperties properties){
		return new ZooKeeperStringCodecImpl(properties.getZooKeeperUrl(), applicationName);
	}
	
}
