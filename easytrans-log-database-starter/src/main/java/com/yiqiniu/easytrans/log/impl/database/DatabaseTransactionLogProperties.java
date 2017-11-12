package com.yiqiniu.easytrans.log.impl.database;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.log.database")
public class DatabaseTransactionLogProperties {
	
	private boolean logCleanEnabled = true;
	
	private int logReservedDays = 14;
	
	private String logCleanTime = "01:20:00";
	
	private Boolean enabled;
	
	private Map<String,String> druid;

	public Map<String, String> getDruid() {
		return druid;
	}

	public void setDruid(Map<String, String> druid) {
		this.druid = druid;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public int getLogReservedDays() {
		return logReservedDays;
	}

	public void setLogReservedDays(int logReservedDays) {
		this.logReservedDays = logReservedDays;
	}

	public String getLogCleanTime() {
		return logCleanTime;
	}

	public void setLogCleanTime(String logCleanTime) {
		this.logCleanTime = logCleanTime;
	}

	public boolean isLogCleanEnabled() {
		return logCleanEnabled;
	}

	public void setLogCleanEnabled(boolean logCleanEnabled) {
		this.logCleanEnabled = logCleanEnabled;
	}

}
