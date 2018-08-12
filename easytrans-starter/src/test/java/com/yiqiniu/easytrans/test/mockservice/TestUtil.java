package com.yiqiniu.easytrans.test.mockservice;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

@Component
public class TestUtil {
	@Resource
	private DataSourceSelector selector;
	
	public JdbcTemplate getJdbcTemplate(String appId,String methodName,EasyTransRequest<?, ?> param) {
		DataSource selectDataSource = selector.selectDataSource(appId, methodName, param);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(selectDataSource);//for test,in real projects,should be cache
		return jdbcTemplate;
	}
	
	
	public JdbcTemplate getJdbcTemplate(String appId,String methodName,long trxId) {
		DataSource selectDataSource = selector.selectDataSource(appId, methodName, trxId);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(selectDataSource);//for test,in real projects,should be cache
		return jdbcTemplate;
	}
	
	private Set<String> exceptionPosition = new HashSet<String>();
	
	void clearException(){
		exceptionPosition.clear();
	}
	
	void setException(String tag){
		exceptionPosition.add(tag);
	}
	
	void checkExcetpionThrow(String tag){
		if(exceptionPosition.contains(tag)){
			throw new RuntimeException("Designed Exception:" + tag);
		}
	}
	
}
