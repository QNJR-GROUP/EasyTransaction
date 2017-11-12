package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.Future;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface EasyTransExecutor{
	/**
	 * 对不同补偿形式的方法进行调用处理
	 * @param sameBusinessCallSeq 业务中多次调用同一个appId,busCode对应的业务时的顺序
	 * @param params 调用对应的业务的参数
	 * @return
	 */
	<P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R  extends Serializable> Future<R> execute(Integer sameBusinessCallSeq, P params);
}
