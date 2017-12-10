package com.yiqiniu.easytrans.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublishResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class RemoteServiceCaller {
	
	private EasyTransRpcConsumer consumer;
	private EasyTransMsgPublisher publisher;
	private ObjectSerializer serializer;
	
	
	public RemoteServiceCaller(EasyTransRpcConsumer consumer, EasyTransMsgPublisher publisher,
			ObjectSerializer serializer) {
		super();
		this.consumer = consumer;
		this.publisher = publisher;
		this.serializer = serializer;
	}

	public <P extends EasyTransRequest<R,?>,R extends Serializable> R call(String appId,String busCode, Integer callSeq,String innerMethod,P params,LogProcessContext logContext){
		return consumer.call(appId, busCode, innerMethod,initEasyTransRequestHeader(callSeq,logContext), params);
	}
	
	public <P extends EasyTransRequest<R,?>,R extends Serializable> void callWithNoReturn(String appId,String busCode, Integer callSeq, String innerMethod,P params,LogProcessContext logContext){
		consumer.callWithNoReturn(appId, busCode, innerMethod,initEasyTransRequestHeader(callSeq,logContext), params);
	}
	
	
	public EasyTransMsgPublishResult publish(String topic, String tag, Integer callSeq, String key, EasyTransRequest<?, ?> request,LogProcessContext logContext){
		return publisher.publish(topic, tag, key, initEasyTransRequestHeader(callSeq,logContext) ,serializer.serialization(request));
	}
	
	private Map<String,Object> initEasyTransRequestHeader(Integer callSeq, LogProcessContext logContext){
		HashMap<String, Object> header = new HashMap<String,Object>();
		header.put(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY, logContext.getTransactionId());
		header.put(EasytransConstant.CallHeadKeys.CALL_SEQ, callSeq);
		return header; 
	}
}
