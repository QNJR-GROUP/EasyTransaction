package com.yiqiniu.easytrans.log.vo.msg;

import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public class MessageRecordContent extends DemiLeftContent {

	private static final long serialVersionUID = 1L;
	
	private EasyTransRequest<?,?> params;

	public EasyTransRequest<?,?> getParams() {
		return params;
	}

	public void setParams(EasyTransRequest<?,?> message) {
		this.params = message;
	}

	@Override
	public int getLogType() {
		return ContentType.MessageRecord.getContentTypeId();
	}
	
}
