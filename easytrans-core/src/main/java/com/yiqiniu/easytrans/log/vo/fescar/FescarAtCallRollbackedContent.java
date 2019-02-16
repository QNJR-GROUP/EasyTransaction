package com.yiqiniu.easytrans.log.vo.fescar;

import com.yiqiniu.easytrans.log.vo.AfterRollBack;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;

@AfterRollBack
public class FescarAtCallRollbackedContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;


	@Override
	public int getLogType() {
		return ContentType.FescarAtRollbacked.getContentTypeId();
	}
}
