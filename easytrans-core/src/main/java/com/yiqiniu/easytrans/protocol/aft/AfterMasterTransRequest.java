package com.yiqiniu.easytrans.protocol.aft;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.AfterTransMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface AfterMasterTransRequest<R extends Serializable> extends EasyTransRequest<R,AfterTransMethodExecutor> {
	
}
