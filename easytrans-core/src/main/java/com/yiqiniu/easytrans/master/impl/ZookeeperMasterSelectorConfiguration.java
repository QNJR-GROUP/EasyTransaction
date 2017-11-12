package com.yiqiniu.easytrans.master.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.master.EasyTransMasterSelector;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.master.zk.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(ZookeeperMasterSelectorProperties.class)
public class ZookeeperMasterSelectorConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMasterSelector.class)
	public ZooKeeperMasterSelectorImpl zooKeeperMasterSelectorImpl(ZookeeperMasterSelectorProperties properties){
		return new ZooKeeperMasterSelectorImpl(properties.getZooKeeperUrl(), applicationName);
	}
	
}
