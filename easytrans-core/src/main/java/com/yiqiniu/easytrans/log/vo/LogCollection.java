package com.yiqiniu.easytrans.log.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *	整合后的LOG
 */
public class LogCollection implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public LogCollection(String appId, String busCode, String trxId,
			List<Content> orderedContents,Date createTime) {
		super();
		this.appId = appId;
		this.busCode = busCode;
		this.trxId = trxId;
		this.orderedContents = orderedContents;
	}

	/**
	 * 记录日志对应的Appid
	 * appId+busCode+trxId表征全局唯一的一个事务
	 */
	private String appId;
	
	/**
	 * 记录日志对应的busCode
	 * appId+busCode+trxId表征全局唯一的一个事务
	 */
	private String busCode;
	
	/**
	 * appId+busCode下唯一的事务ID
	 */
	private String trxId;
	
	private List<Content> orderedContents;
	
	/**
	 * First log write time
	 */
	private Date createTime;
	
	public String getAppId() {
		return appId;
	}

	public String getBusCode() {
		return busCode;
	}

	public String getTrxId() {
		return trxId;
	}

	public List<Content> getOrderedContents() {
		return orderedContents;
	}
	
	public Date getCreateTime() {
		return createTime;
	}

	@Override
	public String toString() {
		return "LogCollection [appId=" + appId + ", busCode=" + busCode + ", trxId=" + trxId + ", orderedContents="
				+ orderedContents + ", createTime=" + createTime + "]";
	}


	
}
