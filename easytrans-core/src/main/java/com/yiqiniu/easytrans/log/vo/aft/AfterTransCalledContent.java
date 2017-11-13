package com.yiqiniu.easytrans.log.vo.aft;

import com.yiqiniu.easytrans.log.vo.AfterCommit;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterCommit
public class AfterTransCalledContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;

	@Override
	public int getLogType() {
		return ContentType.AfterTransCalled.getContentTypeId();
	}
}
