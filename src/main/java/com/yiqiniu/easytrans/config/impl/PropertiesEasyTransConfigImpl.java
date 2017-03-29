package com.yiqiniu.easytrans.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import com.yiqiniu.easytrans.config.EasyTransConifg;

public class PropertiesEasyTransConfigImpl implements EasyTransConifg {

	Properties properties = new Properties();

	@PostConstruct
	private void init(){
		try(InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("server.properties")) {
			properties.load(resourceAsStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getAppId() {
		return properties.getProperty("appid");
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Map<String, String> getExtendConfig() {
		Map map = properties;
		return Collections.unmodifiableMap(map);
	}

	@Override
	public String getExtendConfig(String key) {
		return properties.getProperty(key);
	}

}
