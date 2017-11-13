package com.yiqiniu.easytrans.protocol.cps;

import java.io.Serializable;

import com.yiqiniu.easytrans.executor.CompensableMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public interface CompensableMethodRequest<R extends Serializable> extends EasyTransRequest<R,CompensableMethodExecutor> {
}
