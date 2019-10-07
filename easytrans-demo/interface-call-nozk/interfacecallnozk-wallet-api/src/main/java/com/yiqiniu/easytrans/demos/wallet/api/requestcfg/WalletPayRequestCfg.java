package com.yiqiniu.easytrans.demos.wallet.api.requestcfg;

import com.yiqiniu.easytrans.demos.wallet.api.WalletPayMoneyService.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.WalletPayMoneyService.WalletPayResponseVO;
import com.yiqiniu.easytrans.demos.wallet.api.WalletServiceApiConstant;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;

/**
 * define the calling configuration for WalletPayMoneyService
 */
@BusinessIdentifer(appId=WalletServiceApiConstant.APPID,busCode="pay",rpcTimeOut=2000)
public class WalletPayRequestCfg extends WalletPayRequestVO implements TccMethodRequest<WalletPayResponseVO>{
	private static final long serialVersionUID = 1L;
}
