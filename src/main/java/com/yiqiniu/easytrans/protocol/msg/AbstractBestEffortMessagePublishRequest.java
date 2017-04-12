package com.yiqiniu.easytrans.protocol.msg;


/**
 *	Best effort message
 */
public abstract class AbstractBestEffortMessagePublishRequest implements BestEffortMessagePublishRequest{
	
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
