package com.yiqiniu.easytrans.protocol.msg;

import java.io.Serializable;

public class PublishResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * when it's BestEffortMessage,it has no value
	 */
	private Integer messageContentId;

	public Integer getMessageContentId() {
		return messageContentId;
	}

	public void setMessageContentId(Integer messageContentId) {
		this.messageContentId = messageContentId;
	}

}
