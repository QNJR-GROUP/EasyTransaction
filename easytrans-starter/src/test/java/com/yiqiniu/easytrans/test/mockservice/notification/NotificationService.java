package com.yiqiniu.easytrans.test.mockservice.notification;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.test.mockservice.order.NotReliableOrderMessage;

@Component
public class NotificationService {

	public void sendMsg(NotReliableOrderMessage msg){
		System.out.println(String.format("user:%s used:%s", msg.getUserId(),msg.getAmount()));
	}
	
}
