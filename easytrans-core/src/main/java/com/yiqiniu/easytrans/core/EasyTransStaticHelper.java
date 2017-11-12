package com.yiqiniu.easytrans.core;

import static com.yiqiniu.easytrans.core.EasytransConstant.EscapeChar;
/** 
* @author xudeyou 
*/
public class EasyTransStaticHelper {
	
	
	public static String getTransId(String appId, String busCode, String trxId) {
		
		if(appId.contains(EscapeChar) || busCode.contains(EscapeChar) || trxId.contains(EscapeChar)){
			throw new RuntimeException("can not use '" + EscapeChar + "' in appId,busCode or trxId");
		}
		
		return appId + EscapeChar +busCode + EscapeChar + trxId;
	}
	
	public static String[] getSplitTransId(String transId){
		String[] split = transId.split(EscapeChar);
		if(split.length != 3){
			throw new RuntimeException("illegal Params");
		}
		return split;
	}

}
