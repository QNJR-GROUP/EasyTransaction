package com.yiqiniu.easytrans.log.vo.saga;

import com.yiqiniu.easytrans.log.vo.AfterCommit;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterCommit
public class SagaTccCallConfirmedContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;

	@Override
	public int getLogType() {
		return ContentType.SagaTccCallConfirmed.getContentTypeId();
	}
}
