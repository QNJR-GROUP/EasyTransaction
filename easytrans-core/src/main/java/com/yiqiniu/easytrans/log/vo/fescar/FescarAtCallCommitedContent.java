package com.yiqiniu.easytrans.log.vo.fescar;

import com.yiqiniu.easytrans.log.vo.AfterCommit;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterCommit
public class FescarAtCallCommitedContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;

	@Override
	public int getLogType() {
		return ContentType.FescarAtCommited.getContentTypeId();
	}
}
