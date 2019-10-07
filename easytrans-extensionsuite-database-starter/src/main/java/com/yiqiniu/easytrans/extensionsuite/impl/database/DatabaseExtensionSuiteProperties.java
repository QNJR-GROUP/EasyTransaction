package com.yiqiniu.easytrans.extensionsuite.impl.database;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 
* @author xudeyou 
*/

@ConfigurationProperties(prefix="easytrans.extensionsuite.database")
public class DatabaseExtensionSuiteProperties {
	
	private Boolean enabled;
	
	private String tablePrefix;
	
	private int logReservedDays = 14;
    
    private String logCleanTime = "01:20:00";
    
    private Map<String,String> dbSetting;


	public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
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

    public Map<String, String> getDbSetting() {
        return dbSetting;
    }

    public void setDbSetting(Map<String, String> dbSetting) {
        this.dbSetting = dbSetting;
    }

}
