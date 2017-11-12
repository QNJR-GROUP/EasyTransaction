package com.yiqiniu.easytrans.test.mockservice.express.easytrans;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.aft.AfterMasterTransMethod;
import com.yiqiniu.easytrans.protocol.aft.AfterMasterTransRequest;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.express.ExpressService;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.AfterMasterTransMethodResult;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.ExpressDeliverAfterTransMethodRequest;

@Component
public class ExpressDeliverAfterTransMethod implements AfterMasterTransMethod<ExpressDeliverAfterTransMethodRequest, AfterMasterTransMethodResult>{
	
	public static final String BUSINESS_CODE = "noticeExpress";

	@Resource
	private ExpressService service;
	
	@Override
	public AfterMasterTransMethodResult afterTransaction(ExpressDeliverAfterTransMethodRequest param) {
		return service.afterTransaction(param);
	}
	
	@BusinessIdentifer(appId=Constant.APPID,busCode=BUSINESS_CODE)
	public static class ExpressDeliverAfterTransMethodRequest implements AfterMasterTransRequest<AfterMasterTransMethodResult>{

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
	
	public static class AfterMasterTransMethodResult extends EasyTransResult{
		private static final long serialVersionUID = 1L;
		
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}
}
