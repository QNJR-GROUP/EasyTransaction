package com.yiqiniu.easytrans.rpc.impl.rest;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.rpc.rest-ribbon")
public class RestRibbonEasyTransRpcProperties {
	private Boolean enabled;
	private RestProviderProperties provider;
	private Map<String,RestConsumerProperties> consumer;
	
	public RestProviderProperties getProvider() {
		return provider;
	}

	public void setProvider(RestProviderProperties provider) {
		this.provider = provider;
	}

	public Map<String, RestConsumerProperties> getConsumer() {
		return consumer;
	}

	public void setConsumer(Map<String, RestConsumerProperties> consumer) {
		this.consumer = consumer;
	}

	public static class RestProviderProperties{
		private String context;

		public String getContext() {
			return context;
		}

		public void setContext(String context) {
			this.context = context;
		}
	}
	

	public static class RestConsumerProperties {

		private String context;

		public String getContext() {
			return context;
		}

		public void setContext(String context) {
			this.context = context;
		}
	
	}
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
}
