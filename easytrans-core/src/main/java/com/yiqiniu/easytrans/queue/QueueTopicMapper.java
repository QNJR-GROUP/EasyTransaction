package com.yiqiniu.easytrans.queue;

/**
 * 
 * @author xudeyou
 *
 */
public interface QueueTopicMapper {
	
	/**
	 * 
	 * @param appid
	 * @param busCode
	 * @return 字符串数组，第一个为topic,第二个为tag
	 */
	String[] mapToTopicTag(String appid, String busCode);
	
	/**
	 * 
	 * @param topic
	 * @param tag
	 * @return 字符串数组，第一个为appId,第二个为busCode
	 */
	String[] mapToAppIdBusCode(String topic,String tag);
}
