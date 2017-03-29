package com.yiqiniu.easytrans.rpc;

import java.io.Serializable;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public interface EasyTransRpcConsumer {
	<P extends EasyTransRequest<R,?>,R extends Serializable> R call(String appId,String busCode,String innerMethod,P params);
	<P extends EasyTransRequest<R,?>,R extends Serializable> void callWithNoReturn(String appId,String busCode,String innerMethod,P params);
}
