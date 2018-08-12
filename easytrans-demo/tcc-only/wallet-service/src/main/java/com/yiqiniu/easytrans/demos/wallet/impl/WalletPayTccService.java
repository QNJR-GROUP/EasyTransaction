package com.yiqiniu.easytrans.demos.wallet.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.demos.wallet.api.etcfg.WalletPayCfg;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;
import com.yiqiniu.easytrans.protocol.tcc.TccMethod;

@Component
public class WalletPayTccService implements TccMethod<WalletPayCfg, WalletPayResponseVO>{

	public static final String METHOD_NAME="pay";
	
	@Resource
	private WalletService wlletService;

	@Override
	public WalletPayResponseVO doTry(WalletPayCfg param) {
		return wlletService.doTryPay(param);
	}

	@Override
	public void doConfirm(WalletPayCfg param) {
		wlletService.doConfirmPay(param);
	}


	@Override
	public void doCancel(WalletPayCfg param) {
		wlletService.doCancelPay(param);
	}
	

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}
}
