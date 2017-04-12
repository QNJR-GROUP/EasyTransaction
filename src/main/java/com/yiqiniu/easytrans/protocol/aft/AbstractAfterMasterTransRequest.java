package com.yiqiniu.easytrans.protocol.aft;

import java.io.Serializable;



public abstract class AbstractAfterMasterTransRequest<R extends Serializable> implements AfterMasterTransRequest<R>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private TransactionId parentTrxId;

	public TransactionId getParentTrxId() {
		return parentTrxId;
	}

	public void setParentTrxId(TransactionId parentTrxId) {
		this.parentTrxId = parentTrxId;
	}
	
	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}
}
