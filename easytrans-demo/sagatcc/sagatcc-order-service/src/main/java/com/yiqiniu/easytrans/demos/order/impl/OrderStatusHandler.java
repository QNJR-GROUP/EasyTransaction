package com.yiqiniu.easytrans.demos.order.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethod;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethodRequest;

public class OrderStatusHandler {
	
	@BusinessIdentifer(appId=Constant.APPID,busCode="updateOrderStatus")
	public static class UpdateOrderStatus implements SagaTccMethodRequest {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private int orderId;

		public int getOrderId() {
			return orderId;
		}

		public void setOrderId(int orderId) {
			this.orderId = orderId;
		}
	}
	
	@Component
	public static class UpdateOrderStatusMethods implements SagaTccMethod<UpdateOrderStatus>{

		@Autowired
		private OrderService orderService;
		
		@Override
		public int getIdempotentType() {
			return IDENPOTENT_TYPE_FRAMEWORK;
		}

		@Override
		public void sagaTry(UpdateOrderStatus param) {
			//DO NOTHING
		}

		@Override
		public void sagaConfirm(UpdateOrderStatus param) {
			orderService.confirmOrder(param.getOrderId());
		}

		@Override
		public void sagaCancel(UpdateOrderStatus param) {
			orderService.cancelOrder(param.getOrderId());
		}
		
	}

}
