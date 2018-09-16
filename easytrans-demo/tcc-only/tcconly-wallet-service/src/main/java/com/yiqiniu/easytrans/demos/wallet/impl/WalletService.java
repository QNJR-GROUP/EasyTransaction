package com.yiqiniu.easytrans.demos.wallet.impl;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;

@Component
public class WalletService {
	
	@Resource
	private EasyTransFacade transaction;
	@Resource
	private JdbcTemplate jdbcTemplate;
	
	@Transactional
	public WalletPayResponseVO doTryPay(WalletPayRequestVO param) {
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount + ? where user_id = ? and (total_amount - freeze_amount) >= ?;", 
				param.getPayAmount(),param.getUserId(),param.getPayAmount());
		
		if(update != 1){
			throw new RuntimeException("can not find specific user id or have not enought money");
		}
		
		WalletPayResponseVO walletPayTccMethodResult = new WalletPayResponseVO();
		walletPayTccMethodResult.setFreezeAmount(param.getPayAmount());
		return walletPayTccMethodResult;
	}
	
	@Transactional
	public void doConfirmPay(WalletPayRequestVO param) {
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount - ?, total_amount = total_amount - ? where user_id = ?;", 
				param.getPayAmount(),param.getPayAmount(),param.getUserId());
		
		if(update != 1){
			throw new RuntimeException("unknow Exception!");
		}
	}
	
	@Transactional
	public void doCancelPay(WalletPayRequestVO param) {
		int update = jdbcTemplate.update("update `wallet` set freeze_amount = freeze_amount - ? where user_id = ?;", 
				param.getPayAmount(),param.getUserId());
		if(update != 1){
			throw new RuntimeException("unknow Exception!");
		}
	}
	

}
