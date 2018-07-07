package com.yiqiniu.easytrans.queue;

public class IdenticalQueueTopicMapper implements QueueTopicMapper {

	@Override
	public String[] mapToTopicTag(String appid, String busCode) {
		return new String[]{appid,busCode};
	}

	@Override
	public String[] mapToAppIdBusCode(String topic, String tag) {
		return new String[]{topic,tag};
	}

}
