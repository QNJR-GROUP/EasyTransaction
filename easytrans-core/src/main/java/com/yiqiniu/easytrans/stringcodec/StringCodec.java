package com.yiqiniu.easytrans.stringcodec;

/**
 * turn a repeated string to a id number
 * @author xu deyou
 *
 */
public interface StringCodec {
	
	/**
	 * find the id for the value,if not found ,then encode and save it<br/>
	 * 
	 * when the value is null,then it will return null
	 * 
	 * @param stringType
	 * @param value
	 * @return
	 */
	Integer findId(String stringType, String value);
	
	/**
	 * find the correspond string<br/>
	 * it will throw exception when string not found
	 * @param stringType
	 * @param id
	 * @return
	 */
	String findString(String stringType, int id);
	
}
