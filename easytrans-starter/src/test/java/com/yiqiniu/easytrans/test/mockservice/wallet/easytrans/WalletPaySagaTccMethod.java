package com.yiqiniu.easytrans.test.mockservice.wallet.easytrans;

import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethod;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethodRequest;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.order.OrderService;
import com.yiqiniu.easytrans.test.mockservice.wallet.WalletService;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPaySagaTccMethod.WalletPaySagaTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodRequest;

@Component
public class WalletPaySagaTccMethod implements SagaTccMethod<WalletPaySagaTccMethodRequest>{

	public static final String METHOD_NAME="sagaPay";
	
	@Resource
	private WalletService wlletService;

	
	@Override
	public void sagaTry(WalletPaySagaTccMethodRequest param) {
		OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_SAGA_TRY);
		WalletPayTccMethodRequest serviceRequest = convert2ServiceParam(param);
		wlletService.doTryPay(serviceRequest);
	}

	private WalletPayTccMethodRequest convert2ServiceParam(WalletPaySagaTccMethodRequest param) {
		WalletPayTccMethodRequest serviceRequest = new WalletPayTccMethodRequest();
		BeanUtils.copyProperties(param, serviceRequest);
		return serviceRequest;
	}

	@Override
	public void sagaConfirm(WalletPaySagaTccMethodRequest param) {
		OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_SUCCESS_MASTER_TRANS);
		wlletService.doConfirmPay(convert2ServiceParam(param));
	}

	@Override
	public void sagaCancel(WalletPaySagaTccMethodRequest param) {
		wlletService.doCancelPay(convert2ServiceParam(param));
	}

	
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

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}


}
