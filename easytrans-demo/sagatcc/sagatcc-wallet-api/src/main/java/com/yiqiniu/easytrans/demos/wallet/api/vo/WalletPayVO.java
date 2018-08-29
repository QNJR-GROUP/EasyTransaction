package com.yiqiniu.easytrans.demos.wallet.api.vo;

import java.io.Serializable;

import com.yiqiniu.easytrans.demos.wallet.api.WalletServiceApiConstant;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethodRequest;

public class WalletPayVO {

	@BusinessIdentifer(appId=WalletServiceApiConstant.APPID,busCode="sagaPay")
	public static class WalletPayRequestVO implements Serializable,SagaTccMethodRequest {

		private static final long serialVersionUID = 1L;


		private Integer userId;
		
		private Long payAmount;

		public Long getPayAmount() {
			return payAmount;
		}

		public void setPayAmount(Long payAmount) {
			this.payAmount = payAmount;
		}

		public Integer getUserId() {
			return userId;
		}

		public void setUserId(Integer userId) {
			this.userId = userId;
		}
	}

}
