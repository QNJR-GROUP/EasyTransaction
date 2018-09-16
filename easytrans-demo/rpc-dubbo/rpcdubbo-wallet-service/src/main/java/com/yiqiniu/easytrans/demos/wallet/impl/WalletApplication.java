package com.yiqiniu.easytrans.demos.wallet.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.yiqiniu.easytrans.EnableEasyTransaction;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.rpc.impl.dubbo.DubboServiceCustomizationer;
import com.yiqiniu.easytrans.rpc.impl.dubbo.EnableRpcDubboImpl;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
@EnableRpcDubboImpl
public class WalletApplication {
	public static void main(String[] args) {
		SpringApplication.run(WalletApplication.class, args);
		
		
		//build a non-daemon thread to keep Process alive
		new Thread() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(100000);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}.start();
	}
	
	
	/**
	 * This is an optional bean, you can modify Dubbo reference here to change the behavior of consumer
	 */
	@Bean
	public DubboServiceCustomizationer dubboProviderCustomizationer() {
		return new DubboServiceCustomizationer() {
			
			@Override
			public void customDubboService(BusinessIdentifer businessIdentifer,
					ServiceConfig<GenericService> serviceConfig) {
				Logger logger = LoggerFactory.getLogger(getClass());
				logger.info("custom dubbo provider!");
			
			}
		};
	}
}
