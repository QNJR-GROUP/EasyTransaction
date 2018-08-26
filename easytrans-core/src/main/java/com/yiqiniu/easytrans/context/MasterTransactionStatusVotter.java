package com.yiqiniu.easytrans.context;

public class MasterTransactionStatusVotter {
	
	private boolean commited = true;
	private Boolean transactionStatus;
	
	/**
	 * 一票否决制度，只要有因素认为全局事务失败，则全局事务失败
	 * one ballot veto
	 */
	public void veto() {
		commited = false;
	}
	
	public boolean getCurrentVoteStatusCommited() {
		return commited;
	}

	public Boolean getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(Boolean transactionStatus) {
		this.transactionStatus = transactionStatus;
	}
}
