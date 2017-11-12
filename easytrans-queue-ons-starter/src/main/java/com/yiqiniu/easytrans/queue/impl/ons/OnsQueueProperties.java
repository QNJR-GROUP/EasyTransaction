package com.yiqiniu.easytrans.queue.impl.ons;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xudeyou
 */

@ConfigurationProperties(prefix = "easytrans.queue.ons")
public class OnsQueueProperties {

	/*
	 * PropertyKeyConst.ONSAddr,阿里云身份验证，在阿里云服务器管理控制台创建
	 * PropertyKeyConst.AccessKey, 阿里云身份验证，在阿里云服务器管理控制台创建
	 * PropertyKeyConst.SecretKey, 此处以公有云生产环境为例 
	 * PropertyKeyConst.ProducerId
	 */
	private Map<Object, Object> publisher;

	/*
	 * 以下为必填字段
	 * 
	 * PropertyKeyConst.ONSAddr// 阿里云身份验证，在阿里云服务器管理控制台创建
	 * PropertyKeyConst.AccessKey // 阿里云身份验证，在阿里云服务器管理控制台创建
	 * PropertyKeyConst.SecretKey// 此处以公有云生产环境为例 
	 * PropertyKeyConst.ConsumerId//
	 * 您在控制台创建的Consumer ID
	 */
	private Map<Object, Object> consumer;
	
	private Boolean enabled;
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Map<Object, Object> getPublisher() {
		return publisher;
	}

	public void setPublisher(Map<Object, Object> publisher) {
		this.publisher = publisher;
	}

	public Map<Object, Object> getConsumer() {
		return consumer;
	}

	public void setConsumer(Map<Object, Object> consumer) {
		this.consumer = consumer;
	}

}
