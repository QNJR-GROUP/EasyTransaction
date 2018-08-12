package com.yiqiniu.easytrans.core;

import java.io.Serializable;
import java.util.concurrent.Future;

import com.yiqiniu.easytrans.executor.EasyTransExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface EasyTransFacade {
	
	public void startEasyTrans(String busCode,long trxId);

	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor, R extends Serializable> Future<R> execute(P params);
}
