package com.yiqiniu.easytrans.test.mockservice.wallet.easytrans;

import javax.annotation.Resource;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.wallet.WalletService;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodResult;

public class WalletPayCascadeTccMethod{

	public static final String METHOD_NAME="payCascade";
	
	@Resource
	private WalletService wlletService;
	
	@BusinessIdentifer(appId=Constant.APPID,busCode=METHOD_NAME,rpcTimeOut=2000)
	public static class WalletPayCascadeTccMethodRequest implements TccMethodRequest<WalletPayTccMethodResult>{

		private static final long serialVersionUID = 1L;
		
		
		private Integer userId;
		
		private Long payAmount;
		
		private Boolean useCoupon;

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

        public Boolean getUseCoupon() {
            return useCoupon;
        }

        public void setUseCoupon(Boolean useCoupon) {
            this.useCoupon = useCoupon;
        }
		
		
	}
}
