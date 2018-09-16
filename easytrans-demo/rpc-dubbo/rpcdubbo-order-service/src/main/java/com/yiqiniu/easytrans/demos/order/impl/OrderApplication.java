package com.yiqiniu.easytrans.demos.order.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.yiqiniu.easytrans.EnableEasyTransaction;
import com.yiqiniu.easytrans.rpc.impl.dubbo.DubboReferanceCustomizationer;
import com.yiqiniu.easytrans.rpc.impl.dubbo.EnableRpcDubboImpl;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
@EnableRpcDubboImpl
public class OrderApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
	
	
	/**
	 * This is an optional bean, you can modify Dubbo reference here to change the behavior of consumer
	 * @return
	 */
	@Bean
	public DubboReferanceCustomizationer dubboConsumerCustomizationer() {
		return new DubboReferanceCustomizationer() {
			
			@Override
			public void customDubboReferance(String appId, String busCode, ReferenceConfig<GenericService> referenceConfig) {
				Logger logger = LoggerFactory.getLogger(getClass());
				logger.info("custom dubbo consumer!");
			}
		};
	}
}
