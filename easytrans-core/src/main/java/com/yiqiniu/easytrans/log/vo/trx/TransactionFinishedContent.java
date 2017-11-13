package com.yiqiniu.easytrans.log.vo.trx;

import com.yiqiniu.easytrans.log.vo.DemiRightContent;


public class TransactionFinishedContent extends DemiRightContent {

	private static final long serialVersionUID = 1L;

	@Override
	public int getLogType() {
		return ContentType.TransactionFininshed.getContentTypeId();
	}
}
