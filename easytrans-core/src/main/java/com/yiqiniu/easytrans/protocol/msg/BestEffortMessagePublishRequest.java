package com.yiqiniu.easytrans.protocol.msg;

import com.yiqiniu.easytrans.executor.BestEffortMessageMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.msg.BestEffortMessagePublishRequest.SerializableVoid;

/**
 *	Best effort message
 */
public interface BestEffortMessagePublishRequest extends EasyTransRequest<SerializableVoid,BestEffortMessageMethodExecutor>{
	
	public static enum SerializableVoid {
		SINGLETON
	}
	
}
