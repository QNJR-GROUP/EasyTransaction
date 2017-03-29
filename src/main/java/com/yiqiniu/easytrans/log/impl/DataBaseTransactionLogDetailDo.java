package com.yiqiniu.easytrans.log.impl;

import java.io.Serializable;
import java.util.Date;

public class DataBaseTransactionLogDetailDo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static String EscapeChar = "#"; 
	
	public static String getTransId(String appId, String busCode, String trxId) {
		
		if(appId.contains(EscapeChar) || busCode.contains(EscapeChar) || trxId.contains(EscapeChar)){
			throw new RuntimeException("can not use '" + EscapeChar + "' in appId,busCode or trxId");
		}
		
		return appId + EscapeChar +busCode + EscapeChar + trxId;
	}
	
	public static String[] getSplitTransId(String transId){
		String[] split = transId.split(EscapeChar);
		if(split.length != 3){
			throw new RuntimeException("illegal Params");
		}
		return split;
	}
	
	private Integer logDetailId;
	private String transLogId;
	private byte[] logDetail;
	private Date createTime;

	public Integer getLogDetailId() {
		return logDetailId;
	}
	public void setLogDetailId(Integer logDetailId) {
		this.logDetailId = logDetailId;
	}
	public String getTransLogId() {
		return transLogId;
	}
	public void setTransLogId(String transLogId) {
		this.transLogId = transLogId;
	}

	public byte[] getLogDetail() {
		return logDetail;
	}

	public void setLogDetail(byte[] logDetail) {
		this.logDetail = logDetail;
	}

	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
}
