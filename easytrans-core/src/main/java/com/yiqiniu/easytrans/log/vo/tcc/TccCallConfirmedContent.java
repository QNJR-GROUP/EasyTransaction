package com.yiqiniu.easytrans.log.vo.tcc;

import com.yiqiniu.easytrans.log.vo.AfterCommit;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterCommit
public class TccCallConfirmedContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;

	@Override
	public int getLogType() {
		return ContentType.TccCallConfirmed.getContentTypeId();
	}
}
