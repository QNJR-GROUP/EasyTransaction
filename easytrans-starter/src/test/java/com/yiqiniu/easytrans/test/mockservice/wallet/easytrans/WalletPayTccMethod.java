package com.yiqiniu.easytrans.test.mockservice.wallet.easytrans;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.tcc.TccMethod;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.order.OrderService;
import com.yiqiniu.easytrans.test.mockservice.wallet.WalletService;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodResult;

@Component
public class WalletPayTccMethod implements TccMethod<WalletPayTccMethodRequest, WalletPayTccMethodResult>{

	public static final String METHOD_NAME="pay";
	
	@Resource
	private WalletService wlletService;

	@Override
	public WalletPayTccMethodResult doTry(WalletPayTccMethodRequest param) {
		return wlletService.doTryPay(param);
	}

	@Override
	public void doConfirm(WalletPayTccMethodRequest param) {
		OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_SUCCESS_MASTER_TRANS);
		wlletService.doConfirmPay(param);
	}


	@Override
	public void doCancel(WalletPayTccMethodRequest param) {
		wlletService.doCancelPay(param);
	}
	
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

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}
}
