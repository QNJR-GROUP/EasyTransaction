package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.Future;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface EasyTransExecutor{
	<P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R  extends Serializable> Future<R> execute(P params);
}
