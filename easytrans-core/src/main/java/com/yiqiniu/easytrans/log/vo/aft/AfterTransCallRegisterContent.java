package com.yiqiniu.easytrans.log.vo.aft;

import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public class AfterTransCallRegisterContent extends DemiLeftContent {

	private static final long serialVersionUID = 1L;
	
	private EasyTransRequest<?,?> params;

	@Override
	public int getLogType() {
		return ContentType.AfterTransCallRegister.getContentTypeId();
	}

	public EasyTransRequest<?,?> getParams() {
		return params;
	}

	public void setParams(EasyTransRequest<?,?> params) {
		this.params = params;
	}
}
