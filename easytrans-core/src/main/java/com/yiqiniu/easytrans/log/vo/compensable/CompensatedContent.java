package com.yiqiniu.easytrans.log.vo.compensable;

import com.yiqiniu.easytrans.log.vo.AfterRollBack;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterRollBack
public class CompensatedContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;

	@Override
	public int getLogType() {
		return ContentType.Compensated.getContentTypeId();
	}
}
