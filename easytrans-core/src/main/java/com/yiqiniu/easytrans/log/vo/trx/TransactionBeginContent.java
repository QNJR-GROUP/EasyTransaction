package com.yiqiniu.easytrans.log.vo.trx;

import com.yiqiniu.easytrans.log.vo.DemiLeftContent;


public class TransactionBeginContent extends DemiLeftContent {

	private static final long serialVersionUID = 1L;
	
	private String pTrxAppId;//上级事务所在的App
	
	private String pTrxId;//上级事务所在的trxId


	@Override
	public int getLogType() {
		return ContentType.TransactionBegin.getContentTypeId();
	}
	
	public String getpTrxAppId() {
		return pTrxAppId;
	}
	
	public void setpTrxAppId(String pTrxAppId) {
		this.pTrxAppId = pTrxAppId;
	}
	
	public String getpTrxId() {
		return pTrxId;
	}
	
	public void setpTrxId(String pTrxId) {
		this.pTrxId = pTrxId;
	}
	
	
}
