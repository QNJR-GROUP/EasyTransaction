package com.yiqiniu.easytrans.demos.wallet.impl;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import com.yiqiniu.easytrans.EnableEasyTransaction;

@SpringBootApplication
@EnableEasyTransaction
@EnableTransactionManagement
public class CouponApplication {
	public static void main(String[] args) {
		SpringApplication.run(CouponApplication.class, args);
	}

	@Bean
	public DataSource dataSourceProxy() {
	    
	    DruidDataSource druidDataSource = new DruidDataSource();
	    druidDataSource.setUrl("jdbc:mysql://localhost:3306/coupon?characterEncoding=UTF-8&useSSL=false");
	    druidDataSource.setUsername("root");
	    druidDataSource.setPassword("123456");
	    
	    return new DataSourceProxy(druidDataSource);
	}
	
}
