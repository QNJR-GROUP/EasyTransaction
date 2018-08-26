package com.yiqiniu.easytrans.datasource;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.TransactionId;

public interface TransStatusLogger {
	
	public static class TransactionStatus{
		public static final int COMMITTED = 0;
		public static final int ROLLBACKED = 1;
		public static final int UNKNOWN = 2;
	}
	
	
	/**
	 * check the master transaction status,the default implement is 
	 * <ul>
	 * <li>if business committed  the record write in recordStatus() will be found in the database</li>
	 * <li>if business roll back the record write in recordStatus() can not be found in the database</li>
	 * </ul>
	 * @param appId
	 * @param trxId
	 * @return null for processing/unknown,false for roll back,true for committed  
	 */
	Boolean checkTransactionStatus(String appId,String busCode,long trxId);
	
	/**
	 * invoke before RPC is executed,to help checkStatus() indicate the final status of a business transaction
	 * @param appId
	 * @param busCode
	 * @param trxId
	 */
	void writeExecuteFlag(String appId, String busCode, long trxId, String pAppId, String pBusCode, Long pTrxId,
			int status);

	/**
	 * 
	 * @param pId
	 * @param request
	 * @param status
	 */
	void updateExecuteFlagForSlaveTrx(TransactionId pId, EasyTransRequest<?, ?> request, int status);

	/**
	 * update master transaction status
	 * @param pId
	 * @param request
	 * @param status
	 */
	int updateMasterTransactionStatus(TransactionId pId, int status);
}
