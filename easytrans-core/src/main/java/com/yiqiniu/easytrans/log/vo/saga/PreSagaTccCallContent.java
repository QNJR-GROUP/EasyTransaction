package com.yiqiniu.easytrans.log.vo.saga;

import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public class PreSagaTccCallContent extends DemiLeftContent {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 调用的参数
	 */
	private EasyTransRequest<?,?> params;

	@Override
	public int getLogType() {
		return ContentType.PreSagaTccCall.getContentTypeId();
	}

	public EasyTransRequest<?,?> getParams() {
		return params;
	}

	public void setParams(EasyTransRequest<?,?> params) {
		this.params = params;
	}

	@Override
	public String toString() {
		return "PreSagaTccCallContent [params=" + params + "]";
	}
	
	
}
