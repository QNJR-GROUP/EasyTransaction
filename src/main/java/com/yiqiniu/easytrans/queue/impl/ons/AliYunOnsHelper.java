package com.yiqiniu.easytrans.queue.impl.ons;

import java.util.Properties;

import javax.annotation.Resource;

import org.springframework.context.annotation.Lazy;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.yiqiniu.easytrans.config.EasyTransConifg;

@Lazy
public class AliYunOnsHelper {
	
	@Resource
	private EasyTransConifg config;

	public String findCfgProducerAccessKey(){
		return config.getExtendConfig("queue.ons.access.key");
	}

	public String findCfgProducerSecrect(){
		return config.getExtendConfig("queue.ons.access.secrect");
	}
	
	// 公有云生产环境：http://onsaddr-internal.aliyun.com:8080/rocketmq/nsaddr4client-internal
	// 公有云公测环境：http://onsaddr-internet.aliyun.com/rocketmq/nsaddr4client-internet
	// 杭州金融云环境：http://jbponsaddr-internal.aliyun.com:8080/rocketmq/nsaddr4client-internal
	// 杭州深圳云环境：http://mq4finance-sz.addr.aliyun.com:8080/rocketmq/nsaddr4client-internal
	public String findCfgOnsSrvAddr(){
		return config.getExtendConfig("queue.ons.srv.addr");
	}

	public String findCfgProducerId(){
			return config.getAppId();
	}
	
	public String findCfgConsumerId(){
		String env = config.getExtendConfig("queue.ons.msg.consumer.prefix");
		env = env == null?"":env;
		return "CID-" + config.getAppId() + env ;
	}

	public Properties getProducerStarupProperties() {
		Properties properties = new Properties();
		setCommonProperties(properties);
		properties.put(PropertyKeyConst.ProducerId, findCfgProducerId()); // 您在控制台创建的Producer ID
		return properties;
	}

	public Properties getConsumerStarupProperties() {
		Properties properties = new Properties();
		setCommonProperties(properties);
		properties.put(PropertyKeyConst.ConsumerId, findCfgConsumerId()); // 您在控制台创建的Producer ID
		return properties;
	}


	private void setCommonProperties(Properties properties) {
		properties.put(PropertyKeyConst.AccessKey, findCfgProducerAccessKey()); // 阿里云身份验证，在阿里云服务器管理控制台创建
		properties.put(PropertyKeyConst.SecretKey, findCfgProducerSecrect()); // 阿里云身份验证，在阿里云服务器管理控制台创建
		properties.put(PropertyKeyConst.ONSAddr,findCfgOnsSrvAddr());// 此处以公有云生产环境为例
	}
	
	

}
