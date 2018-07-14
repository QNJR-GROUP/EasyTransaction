package com.yiqiniu.easytrans.idempotent;

import java.util.Map;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public class DefaultIdempotentTransactionDefinition implements IdempotentTransactionDefinition {

	@Override
	public TransactionDefinition getTransactionDefinition(EasyTransFilterChain filterChain, Map<String, Object> header,
			EasyTransRequest<?, ?> request) {
		return new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
	}

}
