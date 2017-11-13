package com.yiqiniu.easytrans.idempotent.exception;

import com.yiqiniu.easytrans.filter.EasyTransResult;

public class ResultWrapperExeception extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private EasyTransResult result;
	
	public ResultWrapperExeception(EasyTransResult result){
		this.result = result;
	}

	public EasyTransResult getResult() {
		return result;
	}
}
