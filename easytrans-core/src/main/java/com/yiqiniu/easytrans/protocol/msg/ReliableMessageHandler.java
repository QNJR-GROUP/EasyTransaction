package com.yiqiniu.easytrans.protocol.msg;

import com.yiqiniu.easytrans.protocol.MessageBusinessProvider;



public interface ReliableMessageHandler<P extends ReliableMessagePublishRequest> extends MessageBusinessProvider<P> {
}
