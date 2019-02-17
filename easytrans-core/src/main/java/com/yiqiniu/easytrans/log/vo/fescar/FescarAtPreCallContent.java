package com.yiqiniu.easytrans.log.vo.fescar;

import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public class FescarAtPreCallContent extends DemiLeftContent {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 调用的参数
	 */
	private EasyTransRequest<?,?> params;

	@Override
	public int getLogType() {
		return ContentType.FescarAtPreCall.getContentTypeId();
	}

	public EasyTransRequest<?,?> getParams() {
		return params;
	}

	public void setParams(EasyTransRequest<?,?> params) {
		this.params = params;
	}
}
