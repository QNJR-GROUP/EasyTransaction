package com.yiqiniu.easytrans.log.vo.tcc;

import com.yiqiniu.easytrans.log.vo.AfterRollBack;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterRollBack
public class TccCallCancelledContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;


	@Override
	public int getLogType() {
		return ContentType.TccCallCanceled.getContentTypeId();
	}
}
