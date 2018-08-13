package com.yiqiniu.easytrans.demos.order.remoteservice;

import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;

public interface WalletPayMoneyService {
	WalletPayResponseVO pay(WalletPayRequestVO request);
	
	//we can also return future instead of the actual return VO to improve performance
//	Future<WalletPayResponseVO> pay(WalletPayRequestVO request);
}
