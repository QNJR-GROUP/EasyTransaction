package com.yiqiniu.easytrans.log.vo.msg;

import com.yiqiniu.easytrans.log.vo.AfterCommit;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterCommit
public class MessageSentContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;
	
	private String remoteMessageId;
	
	@Override
	public int getLogType() {
		return ContentType.MessageSent.getContentTypeId();
	}

	public String getRemoteMessageId() {
		return remoteMessageId;
	}

	public void setRemoteMessageId(String remoteMessageId) {
		this.remoteMessageId = remoteMessageId;
	}
}
