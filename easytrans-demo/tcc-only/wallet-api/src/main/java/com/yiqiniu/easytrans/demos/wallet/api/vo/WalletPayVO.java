package com.yiqiniu.easytrans.demos.wallet.api.vo;

import com.yiqiniu.easytrans.demos.wallet.api.WalletServiceApiConstant;
import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;

public class WalletPayVO {

	public static final String METHOD_NAME="pay";
	
	@BusinessIdentifer(appId=WalletServiceApiConstant.APPID,busCode=METHOD_NAME,rpcTimeOut=2000)
	public static class WalletPayRequestVO implements TccMethodRequest<WalletPayResponseVO>{

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
	
	public static class WalletPayResponseVO extends EasyTransResult{
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
