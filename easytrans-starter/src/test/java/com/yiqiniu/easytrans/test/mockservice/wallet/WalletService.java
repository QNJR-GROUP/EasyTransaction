package com.yiqiniu.easytrans.test.mockservice.wallet;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodResult;

@Component
public class WalletService {
	
	@Resource
	private TestUtil util;
	
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
	
	public WalletPayTccMethodResult doTryPay(WalletPayTccMethodRequest param) {
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
	
}
