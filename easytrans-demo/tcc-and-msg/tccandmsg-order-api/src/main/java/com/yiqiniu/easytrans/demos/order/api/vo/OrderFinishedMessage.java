package com.yiqiniu.easytrans.demos.order.api.vo;

import com.yiqiniu.easytrans.demos.order.api.OrderServiceApiConstant;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.msg.ReliableMessagePublishRequest;

@BusinessIdentifer(appId=OrderServiceApiConstant.APPID,busCode="orderFinished")
public class OrderFinishedMessage implements ReliableMessagePublishRequest {

	private static final long serialVersionUID = 1L;

	private Integer userId;
	
	private Long orderAmt;
	
	public Long getOrderAmt() {
		return orderAmt;
	}

	public void setOrderAmt(Long orderAmt) {
		this.orderAmt = orderAmt;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}
}
