package com.yiqiniu.easytrans.datasource.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;

public class DefaultTransStatusLoggerImpl implements TransStatusLogger {

	DataSourceSelector selctor;
	
	public DefaultTransStatusLoggerImpl(DataSourceSelector selctor) {
		super();
		this.selctor = selctor;
	}

	private ConcurrentHashMap<DataSource, JdbcTemplate> mapJdbcTemplate = new ConcurrentHashMap<DataSource, JdbcTemplate>();
	
	
	@Override
	public Boolean checkTransactionStatus(String appId, String busCode, String trxId) {
		
		DataSource dataSource = selctor.selectDataSource(appId, busCode, trxId);
		JdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);
		
		//select * for update
		List<Integer> statusList = jdbcTemplate.queryForList("select status from executed_trans where app_id = ? and bus_code = ? and trx_id = ? for update;", new Object[]{appId,busCode,trxId}, Integer.class);
		
		if(statusList == null || statusList.size() == 0) {
			return false;
		} else {
			//it can only be 1 record,because it's search by primary key
			int status = statusList.get(0);
			
			switch(status){
			case 0:
				//parent transaction status unknown
				return null;
			case 1:
				//success
				return true;
			case 2:
				return false;
			default:
				throw new IllegalArgumentException("unknown transaction status:" + status);
			}
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
	public void writeExecuteFlag(String appId, String busCode, String trxId, String pAppId, String pBusCode, String pTrxId, int status) {
		
		Connection connection = DataSourceUtils.getConnection(selctor.selectDataSource(appId, busCode, trxId));
		
		try {
			PreparedStatement prepareStatement = connection.prepareStatement("INSERT INTO `executed_trans` (`app_id`, `bus_code`, `trx_id`,`p_app_id`, `p_bus_code`, `p_trx_id`,`status`) VALUES ( ?, ?, ?, ?, ?, ?, ?);");
			prepareStatement.setString(1, appId);
			prepareStatement.setString(2, busCode);
			prepareStatement.setString(3, trxId);
			prepareStatement.setString(4, pAppId);
			prepareStatement.setString(5, pBusCode);
			prepareStatement.setString(6, pAppId);
			prepareStatement.setInt(7, status);
			
			int executeUpdate = prepareStatement.executeUpdate();
			if(executeUpdate != 1){
				throw new RuntimeException(String.format("insert count(%s) Error!%s %s %s %s %s %s %s", executeUpdate,appId,busCode,trxId,pAppId,pBusCode,pTrxId,status));
			}
		} catch (SQLException e) {
			throw new RuntimeException("insert Sql failed,check whether the same transaction has been executed?",e);
		}
	}

}
