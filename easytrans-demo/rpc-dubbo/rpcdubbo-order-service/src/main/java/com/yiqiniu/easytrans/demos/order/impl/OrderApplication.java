package com.yiqiniu.easytrans.demos.order.impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.yiqiniu.easytrans.EnableEasyTransaction;
import com.yiqiniu.easytrans.rpc.impl.dubbo.EnableRpcDubboImpl;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
@EnableRpcDubboImpl
public class OrderApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}
