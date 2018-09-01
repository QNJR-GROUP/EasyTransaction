package com.yiqiniu.easytrans.demos.point.impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.yiqiniu.easytrans.EnableEasyTransaction;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
public class PointApplication {
	public static void main(String[] args) {
		SpringApplication.run(PointApplication.class, args);
	}
}
