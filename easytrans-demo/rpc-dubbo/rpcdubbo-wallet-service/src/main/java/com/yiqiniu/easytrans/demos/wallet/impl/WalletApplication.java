package com.yiqiniu.easytrans.demos.wallet.impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.yiqiniu.easytrans.EnableEasyTransaction;
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
		}.start();;
	}
}
