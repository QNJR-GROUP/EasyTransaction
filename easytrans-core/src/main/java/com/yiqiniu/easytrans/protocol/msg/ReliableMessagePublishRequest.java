package com.yiqiniu.easytrans.protocol.msg;

import com.yiqiniu.easytrans.executor.ReliableMessageMethodExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

/**
 *	transaction message
 */
public interface ReliableMessagePublishRequest extends EasyTransRequest<PublishResult,ReliableMessageMethodExecutor>{
}
