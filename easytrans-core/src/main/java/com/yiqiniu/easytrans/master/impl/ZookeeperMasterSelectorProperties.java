package com.yiqiniu.easytrans.master.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.master.zk")
public class ZookeeperMasterSelectorProperties {
	
	private Boolean enabled;
	
	private String zooKeeperUrl;

	public String getZooKeeperUrl() {
		return zooKeeperUrl;
	}

	public void setZooKeeperUrl(String zooKeeperUrl) {
		this.zooKeeperUrl = zooKeeperUrl;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

}
