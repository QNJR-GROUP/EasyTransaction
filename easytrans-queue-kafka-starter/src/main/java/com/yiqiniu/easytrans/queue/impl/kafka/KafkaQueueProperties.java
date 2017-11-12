package com.yiqiniu.easytrans.queue.impl.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xudeyou
 */

@ConfigurationProperties(prefix = "easytrans.queue.kafka")
public class KafkaQueueProperties {
	private Boolean enabled;
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
