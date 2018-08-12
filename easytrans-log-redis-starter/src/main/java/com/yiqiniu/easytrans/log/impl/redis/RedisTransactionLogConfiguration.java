package com.yiqiniu.easytrans.log.impl.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ByteFormIdCodec;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.log.redis.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(RedisTransactionLogProperties.class)
public class RedisTransactionLogConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnMissingBean(RedisAsyncCommanderProvider.class)
	public RedisAsyncCommanderProvider redisAsyncCommanderProvider(RedisTransactionLogProperties properties){
		return new RedisAsyncCommanderProvider(properties.getRedisUri());
	}
	
	@Bean
	@ConditionalOnMissingBean(TransactionLogReader.class)
	public TransactionLogReader transactionLogReader(RedisAsyncCommanderProvider cmdProvider,ObjectSerializer serializer,RedisTransactionLogProperties properties, ByteFormIdCodec idCodec){
		return new RedisTransactionLogReaderImpl(applicationName,cmdProvider,serializer,properties.getKeyPrefix(),idCodec);
	}
	
	@Bean
	@ConditionalOnMissingBean(TransactionLogWritter.class)
	public TransactionLogWritter transactionLogWritter(RedisAsyncCommanderProvider cmdProvider,ObjectSerializer serializer, RedisTransactionLogProperties properties, ByteFormIdCodec idCodec){
		return new RedisTransactionLogWritterImpl(cmdProvider,serializer,properties.getKeyPrefix(), idCodec);
	}
	
}
