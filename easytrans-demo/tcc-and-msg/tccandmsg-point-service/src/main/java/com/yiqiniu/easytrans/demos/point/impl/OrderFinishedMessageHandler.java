package com.yiqiniu.easytrans.demos.point.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.demos.order.api.vo.OrderFinishedMessage;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.msg.ReliableMessageHandler;
import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;

@Component
public class OrderFinishedMessageHandler implements ReliableMessageHandler<OrderFinishedMessage>{

	
	@Resource
	private PointService pointService;


	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}

	@Override
	public EasyTransConsumeAction consume(EasyTransRequest<?, ?> request) {
		pointService.addPointForBuying((OrderFinishedMessage) request);
		return EasyTransConsumeAction.CommitMessage;
	}
}
