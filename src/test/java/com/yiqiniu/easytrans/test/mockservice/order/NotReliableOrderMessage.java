package com.yiqiniu.easytrans.test.mockservice.order;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.msg.BestEffortMessagePublishRequest;
import com.yiqiniu.easytrans.test.Constant;

@BusinessIdentifer(appId=Constant.APPID,busCode=NotReliableOrderMessage.BUSINESS_CODE)
public class NotReliableOrderMessage implements BestEffortMessagePublishRequest {

	public static final String BUSINESS_CODE = "NotReliableOrderMsg";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private TransactionId parentTrxId;
	
	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}

	public TransactionId getParentTrxId() {
		return parentTrxId;
	}

	public void setParentTrxId(TransactionId parentTrxId) {
		this.parentTrxId = parentTrxId;
	}
	
	
	
	private Integer userId;
	private Long amount;
	
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}
}
