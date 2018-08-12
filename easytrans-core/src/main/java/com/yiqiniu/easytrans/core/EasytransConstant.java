package com.yiqiniu.easytrans.core;
/** 
* @author xudeyou 
*/
public class EasytransConstant {
	
	public static class PropertiesKeys {
		public static final String APPLICATION_NAME_KEY = "spring.application.name";
	}
	
	public static class CallHeadKeys {
		public static final String PARENT_TRX_ID_KEY = "pTrxId";
		public static final String CALL_SEQ = "cseq";
		public static final String PARENT_TRANSACTION_STATUS = "pTrxSts";
	}
	
	public static class StringCodecKeys{
		public static final String APP_ID = "APP_ID";
		public static final String BUSINESS_CODE = "BUS_CODE";
		public static final String METHOD_NAME = "METHOD_NAME";
	}
	
	public final static String EscapeChar = "_";

}
