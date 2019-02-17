package com.yiqiniu.easytrans.test.mockservice.wallet;

import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.saga.EtSaga;
import com.yiqiniu.easytrans.protocol.tcc.EtTcc;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.coupon.easytrans.UseCouponAutoCpsMethod.UseCouponMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.ExpressDeliverAfterTransMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.order.OrderMessageForCascadingTest;
import com.yiqiniu.easytrans.test.mockservice.order.OrderService;
import com.yiqiniu.easytrans.test.mockservice.order.OrderService.UtProgramedException;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayCascadeTccMethod;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayCascadeTccMethod.WalletPayCascadeTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPaySagaTccMethod.WalletPaySagaTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodResult;

@Component
public class WalletService {
	
	@Resource
	private TestUtil util;

	@Resource
	private EasyTransFacade transaction;
	
	
	private boolean exceptionOccurInCascadeTransactionBusinessEnd = false;
	
	public void setExceptionOccurInCascadeTransactionBusinessEnd(boolean exceptionOccurInCascadeTransactionBusinessEnd) {
		this.exceptionOccurInCascadeTransactionBusinessEnd = exceptionOccurInCascadeTransactionBusinessEnd;
	}

	public int getUserTotalAmount(int userId){
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,(WalletPayTccMethodRequest)null);
		Integer queryForObject = jdbcTemplate.queryForObject("select total_amount from wallet where user_id = ?", Integer.class, userId);
		
		return queryForObject == null?0:queryForObject;
	}
	
	public int getUserFreezeAmount(int userId){
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,(WalletPayTccMethodRequest)null);
		Integer queryForObject = jdbcTemplate.queryForObject("select freeze_amount from wallet where user_id = ?", Integer.class, userId);
		
		return queryForObject == null?0:queryForObject;
	}
	
    @EtTcc(confirmMethod = "annotationConfirmPay", cancelMethod = "annotationCancelPay", idempotentType = BusinessProvider.IDENPOTENT_TYPE_FRAMEWORK)
    public WalletPayTccMethodResult annotationTryPay(WalletPayTccMethodRequest param) {
        return doTryPay(param);
    }
    
    public void annotationConfirmPay(WalletPayTccMethodRequest param) {
        doConfirmPay(param);
    }

    public void annotationCancelPay(WalletPayTccMethodRequest param) {
        doCancelPay(param);
    }
    
    @EtSaga(confirmMethod="sagaConfirm",cancelMethod="sagaCancel",idempotentType=BusinessProvider.IDENPOTENT_TYPE_FRAMEWORK)
    public void sagaTry(WalletPaySagaTccMethodRequest param) {
        WalletPayTccMethodRequest serviceRequest = convert2ServiceParam(param);
        try {
            doTryPay(serviceRequest);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    public void sagaConfirm(WalletPaySagaTccMethodRequest param) {
        doConfirmPay(convert2ServiceParam(param));
    }
    
    public void sagaCancel(WalletPaySagaTccMethodRequest param) {
        doCancelPay(convert2ServiceParam(param));
    }

    private WalletPayTccMethodRequest convert2ServiceParam(WalletPaySagaTccMethodRequest param) {
        WalletPayTccMethodRequest serviceRequest = new WalletPayTccMethodRequest();
        BeanUtils.copyProperties(param, serviceRequest);
        return serviceRequest;
    }


	
	public WalletPayTccMethodResult doTryPay(WalletPayTccMethodRequest param) {
	    
	    OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_SAGA_TRY);

	    
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount + ? where user_id = ? and (total_amount - freeze_amount) >= ?;", 
				param.getPayAmount(),param.getUserId(),param.getPayAmount());
		
		if(update != 1){
			throw new RuntimeException("can not find specific user id or have not enought money");
		}
		
		WalletPayTccMethodResult walletPayTccMethodResult = new WalletPayTccMethodResult();
		walletPayTccMethodResult.setFreezeAmount(param.getPayAmount());
		return walletPayTccMethodResult;
	}
	

	public void doConfirmPay(WalletPayTccMethodRequest param) {
	    
	    OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_SUCCESS_MASTER_TRANS);
	    
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount - ?, total_amount = total_amount - ? where user_id = ?;", 
				param.getPayAmount(),param.getPayAmount(),param.getUserId());
		
		if(update != 1){
			throw new RuntimeException("unknow Exception!");
		}
	}
	
	public void doCancelPay(WalletPayTccMethodRequest param) {
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount - ? where user_id = ?;", 
				param.getPayAmount(),param.getUserId());
		if(update != 1){
			throw new RuntimeException("unknow Exception!");
		}
	}
	
	//----------- Cascading test --------
	@EtTcc(confirmMethod="doCascadeConfirmPay",cancelMethod="doCascadeCancelPay",idempotentType=BusinessProvider.IDENPOTENT_TYPE_FRAMEWORK)
	public WalletPayTccMethodResult doCascadeTryPay(WalletPayCascadeTccMethodRequest param) {
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount + ? where user_id = ? and (total_amount - freeze_amount) >= ?;", 
				param.getPayAmount(),param.getUserId(),param.getPayAmount());
		
		if(update != 1){
			throw new RuntimeException("can not find specific user id or have not enought money");
		}
		
		//start cascading transaction
		transaction.startEasyTrans(WalletPayCascadeTccMethod.METHOD_NAME, System.currentTimeMillis());
		//publish point message with an consumer cascading this transaction 
		OrderMessageForCascadingTest msg = new OrderMessageForCascadingTest();
		msg.setUserId(param.getUserId());
		msg.setAmount(param.getPayAmount());
		transaction.execute(msg);
		
		/**
		 * 
		 */
		ExpressDeliverAfterTransMethodRequest expressDeliverAfterTransMethodRequest = new ExpressDeliverAfterTransMethodRequest();
		expressDeliverAfterTransMethodRequest.setUserId(param.getUserId());
		expressDeliverAfterTransMethodRequest.setPayAmount(param.getPayAmount());
		transaction.execute(expressDeliverAfterTransMethodRequest);
		
		
		if(param.getUseCoupon()) {
		    UseCouponMethodRequest useCouponRequest = new UseCouponMethodRequest();
		    useCouponRequest.setUserId(1);
		    useCouponRequest.setCoupon(1l);
		    transaction.execute(useCouponRequest);
		}
		
		
		checkUtProgramedException();
		
		
		WalletPayTccMethodResult walletPayTccMethodResult = new WalletPayTccMethodResult();
		walletPayTccMethodResult.setFreezeAmount(param.getPayAmount());
		return walletPayTccMethodResult;
	}
	

	public void doCascadeConfirmPay(WalletPayCascadeTccMethodRequest param) {
	    
	    OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_SUCCESS_MASTER_TRANS);

	    
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount - ?, total_amount = total_amount - ? where user_id = ?;", 
				param.getPayAmount(),param.getPayAmount(),param.getUserId());
		
		if(update != 1){
			throw new RuntimeException("unknow Exception!");
		}
	}
	
	public void doCascadeCancelPay(WalletPayCascadeTccMethodRequest param) {
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount - ? where user_id = ?;", 
				param.getPayAmount(),param.getUserId());
		if(update != 1){
			throw new RuntimeException("unknow Exception!");
		}
	}
	
    private void checkUtProgramedException() {
        if (exceptionOccurInCascadeTransactionBusinessEnd) {
            throw new UtProgramedException("exceptionOccurInCascadeTransactionBusinessEnd");
        }
    }
	
}
