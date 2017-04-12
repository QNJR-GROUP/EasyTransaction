package com.yiqiniu.easytrans.protocol.cps;

import java.io.Serializable;



public abstract class AbstractCompensableMethodRequest<R extends Serializable> implements CompensableMethodRequest<R>{
	
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
