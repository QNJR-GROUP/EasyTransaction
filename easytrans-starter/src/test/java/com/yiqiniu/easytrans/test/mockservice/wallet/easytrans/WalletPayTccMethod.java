package com.yiqiniu.easytrans.test.mockservice.wallet.easytrans;

import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;
import com.yiqiniu.easytrans.test.Constant;

//@Component  change to annotation delacre
public class WalletPayTccMethod {

	public static final String METHOD_NAME="pay";
	
	public static class WalletPayTccMethodResult extends EasyTransResult{
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
	
	@BusinessIdentifer(appId=Constant.APPID,busCode=METHOD_NAME,rpcTimeOut=2000)
	public static class WalletPayTccMethodRequest implements TccMethodRequest<WalletPayTccMethodResult>{

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
