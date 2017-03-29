package com.yiqiniu.easytrans.config;

import java.util.Map;

public interface EasyTransConifg {
	
	String getAppId();
	
	/**
	 * get configure for extend components
	 * @param key
	 * @return
	 */
	String getExtendConfig(String key);

	Map<String, String> getExtendConfig();
	
}
