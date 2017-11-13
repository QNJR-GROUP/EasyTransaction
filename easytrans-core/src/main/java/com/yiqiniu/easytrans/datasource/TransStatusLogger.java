package com.yiqiniu.easytrans.datasource;


public interface TransStatusLogger {
	
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
	Boolean checkTransactionStatus(String appId,String busCode,String trxId);
	
	/**
	 * invoke before transaction commit,to help checkStatus() indicate the final status of a business transaction
	 * @param appId
	 * @param busCode
	 * @param trxId
	 */
	void writeExecuteFlag(String appId,String busCode,String trxId);
}
