package com.yiqiniu.easytrans.datasource.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;

public class DefaultTransStatusLoggerImpl implements TransStatusLogger {

	@Resource
	DataSourceSelector selctor;
	
	public void setSelctor(DataSourceSelector selctor) {
		this.selctor = selctor;
	}
	
	private ConcurrentHashMap<DataSource, JdbcTemplate> mapJdbcTemplate = new ConcurrentHashMap<DataSource, JdbcTemplate>();
	
	
	@Override
	public Boolean checkTransactionStatus(String appId, String busCode, String trxId) {
		
		DataSource dataSource = selctor.selectDataSource(appId, busCode, trxId);
		JdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);
		
		//select * for update
		Integer count = jdbcTemplate.queryForObject("select count(1) from executed_trans where app_id = ? and bus_code = ? and trx_id = ? for update;", new Object[]{appId,busCode,trxId}, Integer.class);
		
		if(count == null){
			return null;
		}else if(count >= 1){
			return true;
		}else{
			return false;
		}
	}

	private JdbcTemplate getJdbcTemplate(DataSource dataSource) {
		JdbcTemplate jdbcTemplate = mapJdbcTemplate.get(dataSource);
		if(jdbcTemplate == null){
			jdbcTemplate = new JdbcTemplate(dataSource);
			mapJdbcTemplate.put(dataSource, jdbcTemplate);
		}
		return jdbcTemplate;
	}

	@Override
	public void writeExecuteFlag(String appId, String busCode, String trxId) {
		
		Connection connection = DataSourceUtils.getConnection(selctor.selectDataSource(appId, busCode, trxId));
		
		try {
			PreparedStatement prepareStatement = connection.prepareStatement("INSERT INTO `executed_trans` (`app_id`, `bus_code`, `trx_id`) VALUES ( ?, ?, ?);");
			prepareStatement.setString(1, appId);
			prepareStatement.setString(2, busCode);
			prepareStatement.setString(3, trxId);
			
			int executeUpdate = prepareStatement.executeUpdate();
			if(executeUpdate != 1){
				throw new RuntimeException(String.format("insert count(%s) Error!%s %s %s", executeUpdate,appId,busCode,trxId));
			}
		} catch (SQLException e) {
			throw new RuntimeException("insert Sql failed,check whether the same transaction has been executed?",e);
		}
	}

}
