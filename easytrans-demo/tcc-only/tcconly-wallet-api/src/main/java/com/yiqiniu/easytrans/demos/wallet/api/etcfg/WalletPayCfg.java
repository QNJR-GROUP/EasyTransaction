package com.yiqiniu.easytrans.demos.wallet.api.etcfg;

import com.yiqiniu.easytrans.demos.wallet.api.WalletServiceApiConstant;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;

@BusinessIdentifer(appId=WalletServiceApiConstant.APPID,busCode="pay",rpcTimeOut=2000)
public class WalletPayCfg extends WalletPayRequestVO implements TccMethodRequest<WalletPayResponseVO>{
	private static final long serialVersionUID = 1L;
}
