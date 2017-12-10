package com.yiqiniu.easytrans.filter;

import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.yiqiniu.easytrans.core.EasyTransSynchronizer;
import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.TransactionId;

@Order(5)
public class ParentTrxStatusUpdateFilter implements EasyTransFilter {

	private DataSourceSelector selector;
	private TransStatusLogger transStatusLogger;
	private EasyTransSynchronizer easyTransSynchronizer;

	public ParentTrxStatusUpdateFilter(DataSourceSelector selector, TransStatusLogger transStatusLogger, EasyTransSynchronizer easyTransSynchronizer) {
		super();
		this.selector = selector;
		this.transStatusLogger = transStatusLogger;
		this.easyTransSynchronizer = easyTransSynchronizer;
	}

	public PlatformTransactionManager getTransactionManager(EasyTransFilterChain filterChain,
			EasyTransRequest<?, ?> reqest) {
		return selector.selectTransactionManager(filterChain.getAppId(), filterChain.getBusCode(), reqest);
	}

	@Override
	public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String, Object> header,
			EasyTransRequest<?, ?> request) {

		Integer pTrxStatus = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRANSACTION_STATUS);
		if(!pTrxStatus.equals(com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus.UNKNOWN)){
			// start transaction to update 
			PlatformTransactionManager transactionManager = getTransactionManager(filterChain, request);
			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager,
					new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
			TransactionId pTrxId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
			transactionTemplate.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					TransactionId trxId = pTrxId;
					transStatusLogger.updateExecuteFlagForSlaveTrx(trxId, request, pTrxStatus);
					return null;
				}
			});
			boolean commited = pTrxStatus.equals(com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus.COMMITTED);
			//may be concurrent,but it's ok
			easyTransSynchronizer.cascadeExecuteCachedTransaction(pTrxId, commited);
		}
		
		return filterChain.invokeFilterChain(header, request);
	}
}
