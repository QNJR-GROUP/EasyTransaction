package com.yiqiniu.easytrans.demos.order.impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.yiqiniu.easytrans.EnableEasyTransaction;
import com.yiqiniu.easytrans.demos.order.remoteservice.WalletPayMoneyService;
import com.yiqiniu.easytrans.demos.wallet.api.etcfg.WalletPayCfg;
import com.yiqiniu.easytrans.util.CallWrappUtil;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
public class OrderApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
	
	
	@Bean
	public WalletPayMoneyService payService(CallWrappUtil util) {
		return util.createTransactionCallInstance(WalletPayMoneyService.class, WalletPayCfg.class);
	}
	
}
