package com.yiqiniu.easytrans.log.vo.saga;

import com.yiqiniu.easytrans.log.vo.AfterRollBack;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterRollBack
public class SagaTccCallCancelledContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;


	@Override
	public int getLogType() {
		return ContentType.SagaTccCallCanceled.getContentTypeId();
	}
}
