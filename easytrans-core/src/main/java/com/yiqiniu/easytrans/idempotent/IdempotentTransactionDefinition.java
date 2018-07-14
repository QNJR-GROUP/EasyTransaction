package com.yiqiniu.easytrans.idempotent;

import java.util.Map;

import org.springframework.transaction.TransactionDefinition;

import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

/**
 * 
 * 用于控制幂等启动的事务的隔离级别等定义
 * @author xudeyou
 *
 */
public interface IdempotentTransactionDefinition {
	TransactionDefinition getTransactionDefinition(EasyTransFilterChain filterChain, Map<String,Object> header, EasyTransRequest<?, ?> request);
}