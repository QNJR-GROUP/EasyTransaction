package com.yiqiniu.easytrans.test.mockservice.point.easytrans;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.msg.ReliableMessageHandler;
import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;
import com.yiqiniu.easytrans.test.mockservice.order.OrderMessage;
import com.yiqiniu.easytrans.test.mockservice.point.PointService;

@Component
public class PointOrderSuccessConsumer implements ReliableMessageHandler<OrderMessage> {

	@Resource
	private PointService pointService;
	


	@Override
	public EasyTransConsumeAction consume(EasyTransRequest<?, ?> request) {

		pointService.addPointForBuying((OrderMessage) request);
		return EasyTransConsumeAction.CommitMessage;
//		return EasyTransConsumeAction.ReconsumeLater;
	}

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}

}
