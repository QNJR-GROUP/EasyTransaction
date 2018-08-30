package com.yiqiniu.easytrans.demos.wallet.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.demos.wallet.api.WalletPayMoneyService.WalletPayResponseVO;
import com.yiqiniu.easytrans.demos.wallet.api.requestcfg.WalletPayRequestCfg;
import com.yiqiniu.easytrans.protocol.tcc.TccMethod;

@Component
public class WalletPayTccService implements TccMethod<WalletPayRequestCfg, WalletPayResponseVO>{

	public static final String METHOD_NAME="pay";
	
	@Resource
	private WalletService wlletService;

	@Override
	public WalletPayResponseVO doTry(WalletPayRequestCfg param) {
		return wlletService.doTryPay(param);
	}

	@Override
	public void doConfirm(WalletPayRequestCfg param) {
		wlletService.doConfirmPay(param);
	}


	@Override
	public void doCancel(WalletPayRequestCfg param) {
		wlletService.doCancelPay(param);
	}
	

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}
}
