package com.yiqiniu.easytrans.log.impl.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.log.redis")
public class RedisTransactionLogProperties {
	private Boolean enabled;
	private String redisUri;
	private String keyPrefix = "";
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRedisUri() {
		return redisUri;
	}

	public void setRedisUri(String redisUri) {
		this.redisUri = redisUri;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}
	
	
}
