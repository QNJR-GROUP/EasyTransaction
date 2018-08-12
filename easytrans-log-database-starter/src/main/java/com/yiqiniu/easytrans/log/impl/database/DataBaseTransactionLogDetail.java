package com.yiqiniu.easytrans.log.impl.database;

import java.io.Serializable;
import java.util.Date;

public class DataBaseTransactionLogDetail implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Integer logDetailId;
	private byte[] transLogId;
	private byte[] logDetail;
	private Date createTime;

	public Integer getLogDetailId() {
		return logDetailId;
	}
	public void setLogDetailId(Integer logDetailId) {
		this.logDetailId = logDetailId;
	}
	
	public byte[] getTransLogId() {
		return transLogId;
	}
	public void setTransLogId(byte[] transLogId) {
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
