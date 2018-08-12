package com.yiqiniu.easytrans.demos.wallet.api.vo;

import java.io.Serializable;

public class WalletPayVO {

	
	public static class WalletPayRequestVO implements Serializable {

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
	
	public static class WalletPayResponseVO implements Serializable{
		private static final long serialVersionUID = 1L;
		private Long freezeAmount;
		public Long getFreezeAmount() {
			return freezeAmount;
		}
		public void setFreezeAmount(Long freezeAmount) {
			this.freezeAmount = freezeAmount;
		}
		
		@Override
		public String toString() {
			return "WalletPayTccMethodResult [freezeAmount=" + freezeAmount
					+ "]";
		}
	}

}
