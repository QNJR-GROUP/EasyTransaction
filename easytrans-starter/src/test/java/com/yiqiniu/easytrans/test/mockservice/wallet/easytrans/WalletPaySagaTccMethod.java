package com.yiqiniu.easytrans.test.mockservice.wallet.easytrans;

import javax.annotation.Resource;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethodRequest;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.wallet.WalletService;

//@Component  change to annotation delacre
public class WalletPaySagaTccMethod{

	public static final String METHOD_NAME="sagaPay";
	
	@Resource
	private WalletService wlletService;

	
	@BusinessIdentifer(appId=Constant.APPID,busCode=METHOD_NAME,rpcTimeOut=2000)
	public static class WalletPaySagaTccMethodRequest implements SagaTccMethodRequest{

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
