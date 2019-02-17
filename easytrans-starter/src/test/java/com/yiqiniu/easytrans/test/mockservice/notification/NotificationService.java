package com.yiqiniu.easytrans.test.mockservice.notification;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.msg.EtBestEffortMsg;
import com.yiqiniu.easytrans.test.mockservice.order.NotReliableOrderMessage;

@Component
public class NotificationService {

    @EtBestEffortMsg(idempotentType=BusinessProvider.IDENPOTENT_TYPE_BUSINESS)
	public void sendMsg(NotReliableOrderMessage msg){
		System.out.println(String.format("user:%s used:%s", msg.getUserId(),msg.getAmount()));
	}
	
}
