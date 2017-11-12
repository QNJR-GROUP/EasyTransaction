package com.yiqiniu.easytrans.rpc.impl.dubbo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.rpc.dubbo")
public class DubboEasyTransRpcProperties {
	
	private String dubboZkUrl;
	private String dubboDefaultTimeout;
	private Boolean enabled;
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getDubboZkUrl() {
		return dubboZkUrl;
	}
	public void setDubboZkUrl(String dubboZkUrl) {
		this.dubboZkUrl = dubboZkUrl;
	}
	public String getDubboDefaultTimeout() {
		return dubboDefaultTimeout;
	}
	public void setDubboDefaultTimeout(String dubboDefaultTimeout) {
		this.dubboDefaultTimeout = dubboDefaultTimeout;
	}
	
	

}
