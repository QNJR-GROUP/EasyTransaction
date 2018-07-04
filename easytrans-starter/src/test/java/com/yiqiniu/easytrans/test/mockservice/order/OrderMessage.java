package com.yiqiniu.easytrans.test.mockservice.order;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.msg.ReliableMessagePublishRequest;
import com.yiqiniu.easytrans.test.Constant;

@BusinessIdentifer(appId=Constant.APPID + "Test",busCode=OrderMessage.BUSINESS_CODE)
public class OrderMessage implements ReliableMessagePublishRequest {

	public static final String BUSINESS_CODE = "ReliableOrderMsg";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
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
