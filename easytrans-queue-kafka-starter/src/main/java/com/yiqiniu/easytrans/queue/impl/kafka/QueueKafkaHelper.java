package com.yiqiniu.easytrans.queue.impl.kafka;

import com.yiqiniu.easytrans.core.EasytransConstant;

public class QueueKafkaHelper {
	
	public static String getKafkaTopic(String topic,String tag){
		return topic + EasytransConstant.EscapeChar + tag;
	}
	
	public static String[] getEasyTransTopicAndTag(String kafkaTopic){
		String[] splited = kafkaTopic.split(EasytransConstant.EscapeChar);
		if(splited.length != 2){
			throw new IllegalArgumentException("Error kafkaTopic Name:" + kafkaTopic);
		}
		return splited;
	}

}
