package com.yiqiniu.easytrans.rpc;

import java.io.Serializable;
import java.util.Map;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;


public interface EasyTransRpcConsumer {
	<P extends EasyTransRequest<R,?>,R extends Serializable> R call(String appId,String busCode,String innerMethod,Map<String,Object> header,P params);
	<P extends EasyTransRequest<R,?>,R extends Serializable> void callWithNoReturn(String appId,String busCode,String innerMethod,Map<String,Object> header,P params);
}
