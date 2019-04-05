package com.yiqiniu.easytrans.demos.order.impl;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClientSpecification;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.yiqiniu.easytrans.monitor.MonitorConsumerFactory;
import com.yiqiniu.easytrans.rpc.impl.rest.RestRibbonEasyTransRpcConsumerImpl;
import com.yiqiniu.easytrans.rpc.impl.rest.RestRibbonEasyTransRpcProperties;
import com.yiqiniu.easytrans.rpc.impl.rest.RestRibbonMonitorConsumerFactory;

@SpringBootApplication
@EnableConfigurationProperties(RestRibbonEasyTransRpcProperties.class)
//@EnableDubboConfig
public class DashboardApplication {
	public static void main(String[] args) {
		SpringApplication.run(DashboardApplication.class, args);
	}
	

	//------------- for rest ribbon ---------------
	@Bean
	public RestRibbonEasyTransRpcConsumerImpl restRibbonEasyTransRpcConsumerImpl(RestRibbonEasyTransRpcProperties properties, ApplicationContext ctx, List<RibbonClientSpecification> configurations) {
	    return new RestRibbonEasyTransRpcConsumerImpl(properties, null, ctx, configurations);
	}
	
	@Bean
	public MonitorConsumerFactory monitorConsumerFactory(RestRibbonEasyTransRpcConsumerImpl consumer) {
	    return new RestRibbonMonitorConsumerFactory(consumer);
	}
	
	//------------- for dubbo ---------------
//    @Bean
//    public MonitorConsumerFactory monitorConsumerFactory(Optional<ApplicationConfig> applicationConfig, Optional<RegistryConfig> registryConfig, Optional<ProtocolConfig> protocolConfig, Optional<ConsumerConfig> consumerConfig, Optional<ModuleConfig> moduleConfig,
//            Optional<MonitorConfig> monitorConfig, Optional<DubboReferanceCustomizationer> customizationer) {
//        return new DubboMonitorConsumerFactory(applicationConfig, registryConfig, protocolConfig, consumerConfig, moduleConfig, monitorConfig, customizationer);
//    }
}
