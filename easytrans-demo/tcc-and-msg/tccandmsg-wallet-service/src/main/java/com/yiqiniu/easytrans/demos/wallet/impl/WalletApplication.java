package com.yiqiniu.easytrans.demos.wallet.impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.yiqiniu.easytrans.EnableEasyTransaction;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
public class WalletApplication {
	public static void main(String[] args) {
		SpringApplication.run(WalletApplication.class, args);
	}
}
