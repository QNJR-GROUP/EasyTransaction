package com.yiqiniu.easytrans.log;

import java.util.List;

import com.yiqiniu.easytrans.log.vo.Content;

/**
 * Because in TransactionLogReader will find unfinished transaction logs,
 * so the writer implement should record which transaction does not end.
 */
public interface TransactionLogWritter {

	/**
	 * append logs to specific transaction
	 * @param appId
	 * @param busCode unique busCode in appId
	 * @param trxId unique trxId in appId+busCode
	 * @param newOrderedContent appending logs
	 * @param finished is the transaction log complete.
	 */
	void appendTransLog(String appId,String busCode,String trxId,List<Content> newOrderedContent,boolean finished);

}
