package com.yiqiniu.easytrans.recovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.recovery")
public class ConsistentGuardianDaemonProperties {
	
	
	/**
	 * recover execute interval
	 */
	private Integer executeInterval = 5;
	/**
	 * transaction error recover per size
	 */
	private Integer pageSize = 300;
	/**
	 * first transaction recover execute delay seconds
	 */
	private Integer delay = 60;
	/**
	 * enable recover
	 */
	private Boolean enabled = true;
	
	
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	public Integer getExecuteInterval() {
		return executeInterval;
	}
	public void setExecuteInterval(Integer executeInterval) {
		this.executeInterval = executeInterval;
	}
	public Integer getPageSize() {
		return pageSize;
	}
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	public Integer getDelay() {
		return delay;
	}
	public void setDelay(Integer delay) {
		this.delay = delay;
	}

	
	
}
