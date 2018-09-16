package com.yiqiniu.easytrans.test;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.druid.pool.DruidDataSource;
import com.yiqiniu.easytrans.EnableEasyTransaction;
import com.yiqiniu.easytrans.queue.QueueTopicMapper;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingApi;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingRequestCfg;
import com.yiqiniu.easytrans.util.CallWrapUtil;

@SpringBootApplication
@EnableEasyTransaction
@ComponentScan(basePackages={"com.yiqiniu.easytrans.test.mockservice"})
//@EnableRpcRestRibbonImpl default
//@EnableLogDatabaseImpl default
//@EnableQueueKafkaImpl default
//@EnableQueueOnsImpl
//@EnableRpcDubboImpl
//@EnableLogRedisImpl
@EnableTransactionManagement
@EnableAutoConfiguration(exclude=DataSourceAutoConfiguration.class)
public class EasyTransTestConfiguration {
	
	@Bean
	public AccountingApi accountingApi(CallWrapUtil util) {
		return util.createTransactionCallInstance(AccountingApi.class, AccountingRequestCfg.class);
	}
	
	/**
	 * 本BEAN用于配置appid+busCode的映射关系，如果不提供该bean则使用默认实现IdenticalQueueTopicMapper
	 * @return
	 */
	@Bean
	public QueueTopicMapper mapper(){
		return new QueueTopicMapper() {
			
			@Override
			public String[] mapToTopicTag(String appid, String busCode) {
				return new String[]{"TestPrefix" + appid, busCode};
			}
			
			@Override
			public String[] mapToAppIdBusCode(String topic, String tag) {
				return new String[]{topic.replace("TestPrefix", ""), tag};
			}
		};
	}
	
	@Component
	@ConfigurationProperties(prefix="easytrans.test.database")
	public static class EasyTransTestProperties {
		private String url;
		private String username;
		private String password;
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
	}
	
	/**
	 * 创建多个数据源来模拟分布式服务环境
	 * @param properties
	 * @return
	 */
	private DataSource createDatasource(EasyTransTestProperties properties) {
		DruidDataSource ds = new DruidDataSource();
		ds.setUrl(properties.getUrl());
		ds.setUsername(properties.getUsername());
		ds.setPassword(properties.getPassword());
		ds.setMaxActive(10);
		ds.setInitialSize(1);
		ds.setMinIdle(1);
		ds.setPoolPreparedStatements(true);
		return ds;
	}
	
	//--------
	@Bean
	public DataSource whole(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	
	@Bean
	public JdbcTemplate wholeJdbcTemplate(DataSource whole){
		return new JdbcTemplate(whole);
	}
	
	@Bean
	public DataSourceTransactionManager wholeTransactionManager(DataSource whole){
		return new DataSourceTransactionManager(whole);
	}
	
	//--------
	@Bean
	public DataSource buySth(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	@Bean
	public JdbcTemplate buySthJdbcTemplate(DataSource buySth){
		return new JdbcTemplate(buySth);
	}
	@Bean
	public DataSourceTransactionManager buySthTransactionManager(DataSource buySth){
		return new DataSourceTransactionManager(buySth);
	}
	

	//--------
	@Bean
	public DataSource pay(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	@Bean
	public JdbcTemplate payJdbcTemplate(DataSource pay){
		return new JdbcTemplate(pay);
	}
	@Bean
	public DataSourceTransactionManager payTransactionManager(DataSource pay){
		return new DataSourceTransactionManager(pay);
	}
	
	
	//--------
	@Bean
	public DataSource accounting(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	@Bean
	public JdbcTemplate accountingJdbcTemplate(DataSource accounting){
		return new JdbcTemplate(accounting);
	}
	@Bean
	public DataSourceTransactionManager accountingTransactionManager(DataSource accounting){
		return new DataSourceTransactionManager(accounting);
	}
	
	//--------
	@Bean
	public DataSource ReliableOrderMsg(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	@Bean
	public JdbcTemplate ReliableOrderMsgJdbcTemplate(DataSource ReliableOrderMsg){
		return new JdbcTemplate(ReliableOrderMsg);
	}
	@Bean
	public DataSourceTransactionManager ReliableOrderMsgTransactionManager(DataSource ReliableOrderMsg){
		return new DataSourceTransactionManager(ReliableOrderMsg);
	}
	
	//--------
	@Bean
	public DataSource NotReliableOrderMsg(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	@Bean
	public JdbcTemplate NotReliableOrderMsgJdbcTemplate(DataSource NotReliableOrderMsg){
		return new JdbcTemplate(NotReliableOrderMsg);
	}
	@Bean
	public DataSourceTransactionManager NotReliableOrderMsgTransactionManager(DataSource NotReliableOrderMsg){
		return new DataSourceTransactionManager(NotReliableOrderMsg);
	}
	
	//--------
	@Bean
	public DataSource noticeExpress(EasyTransTestProperties properties){
		return createDatasource(properties);
	}
	@Bean
	public JdbcTemplate noticeExpressJdbcTemplate(DataSource noticeExpress){
		return new JdbcTemplate(noticeExpress);
	}
	@Bean
	public DataSourceTransactionManager noticeExpressTransactionManager(DataSource noticeExpress){
		return new DataSourceTransactionManager(noticeExpress);
	}
	
	
	
	
}
