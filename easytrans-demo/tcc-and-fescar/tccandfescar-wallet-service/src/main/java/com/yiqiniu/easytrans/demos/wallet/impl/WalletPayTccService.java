package com.yiqiniu.easytrans.demos.wallet.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;
import com.yiqiniu.easytrans.protocol.tcc.TccMethod;

@Component
public class WalletPayTccService implements TccMethod<WalletPayRequestVO, WalletPayResponseVO>{

	public static final String METHOD_NAME="pay";
	
	@Resource
	private WalletService wlletService;

	@Override
	public WalletPayResponseVO doTry(WalletPayRequestVO param) {
		return wlletService.doTryPay(param);
	}

	@Override
	public void doConfirm(WalletPayRequestVO param) {
		wlletService.doConfirmPay(param);
	}


	@Override
	public void doCancel(WalletPayRequestVO param) {
		wlletService.doCancelPay(param);
	}
	

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}
}
