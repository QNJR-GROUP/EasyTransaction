package com.yiqiniu.easytrans.datasource.impl;

import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.APP_ID;
import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.BUSINESS_CODE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.StringUtils;

import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.stringcodec.StringCodec;

public class DefaultTransStatusLoggerImpl implements TransStatusLogger {

	private String updateTransStatus = "UPDATE `executed_trans` SET `status` =  ? WHERE  `p_app_id` =  ? AND `p_bus_code` =  ? AND `p_trx_id` = ?;";
	private String insertExecutedTag = "INSERT INTO `executed_trans` (`app_id`, `bus_code`, `trx_id`,`p_app_id`, `p_bus_code`, `p_trx_id`,`status`) VALUES ( ?, ?, ?, ?, ?, ?, ?);";
	private String checkTransExecuted = "select status from executed_trans where app_id = ? and bus_code = ? and trx_id = ? for update;";

	private DataSourceSelector selctor;
	
	private StringCodec codec;
	
	public DefaultTransStatusLoggerImpl(DataSourceSelector selctor, StringCodec codec, String tablePrefix) {
		super();
		this.selctor = selctor;
		this.codec = codec;
		
		if(!StringUtils.isEmpty(tablePrefix)) {
			tablePrefix = tablePrefix.trim();
			updateTransStatus = updateTransStatus.replace("executed_trans", tablePrefix + "executed_trans");
			insertExecutedTag = insertExecutedTag.replace("executed_trans", tablePrefix + "executed_trans");
			checkTransExecuted = checkTransExecuted.replace("executed_trans", tablePrefix + "executed_trans");
		}
	}

	private ConcurrentHashMap<DataSource, JdbcTemplate> mapJdbcTemplate = new ConcurrentHashMap<DataSource, JdbcTemplate>();
	
	
	@Override
	public Boolean checkTransactionStatus(String appId, String busCode, long trxId) {
		
		DataSource dataSource = selctor.selectDataSource(appId, busCode, trxId);
		JdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);
		
		//select * for update
		List<Integer> statusList = jdbcTemplate.queryForList(checkTransExecuted, 
				new Object[]{
					codec.findId(APP_ID, appId),
					codec.findId(BUSINESS_CODE, busCode),
					trxId}, 
				Integer.class);
		
		if(statusList == null || statusList.size() == 0) {
			return false;
		} else {
			//it can only be 1 record,because it's search by primary key
			int status = statusList.get(0);
			
			switch(status){
			case TransactionStatus.UNKNOWN:
				//parent transaction status unknown
				return null;
			case TransactionStatus.COMMITTED:
				//success
				return true;
			case TransactionStatus.ROLLBACKED:
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
	public void writeExecuteFlag(String appId, String busCode, long trxId, String pAppId, String pBusCode, Long pTrxId, int status) {
		
		Connection connection = DataSourceUtils.getConnection(selctor.selectDataSource(appId, busCode, trxId));
		
		try {
			PreparedStatement prepareStatement = connection.prepareStatement(insertExecutedTag);
			//
			prepareStatement.setInt(1, codec.findId(APP_ID, appId));
			prepareStatement.setInt(2, codec.findId(BUSINESS_CODE, busCode));
			prepareStatement.setLong(3, trxId);
			if(pAppId != null) {
				prepareStatement.setInt(4, codec.findId(APP_ID, pAppId));
			} else {
				prepareStatement.setNull(4, Types.INTEGER);

			}
			if(pBusCode != null) {
				prepareStatement.setInt(5, codec.findId(BUSINESS_CODE, pBusCode));
			}else {
				prepareStatement.setNull(5, Types.INTEGER);

			}
			if(pTrxId != null) {
				prepareStatement.setLong(6, pTrxId);
			}else {
				prepareStatement.setNull(6, Types.BIGINT);
			}
			prepareStatement.setInt(7, status);
			
			int executeUpdate = prepareStatement.executeUpdate();
			if(executeUpdate != 1){
				throw new RuntimeException(String.format("insert count(%s) Error!%s %s %s %s %s %s %s", executeUpdate,appId,busCode,trxId,pAppId,pBusCode,pTrxId,status));
			}
		} catch (SQLException e) {
			throw new RuntimeException("insert Sql failed,check whether the same transaction has been executed?",e);
		}
	}
	
	@Override
	public void updateExecuteFlagForSlaveTrx(TransactionId pId, EasyTransRequest<?, ?> request, int status) {
		
		BusinessIdentifer businessIdentifer = request.getClass().getAnnotation(BusinessIdentifer.class);
		
		Connection connection = DataSourceUtils.getConnection(selctor.selectDataSource(businessIdentifer.appId(),businessIdentifer.busCode(),request));
		
		try {
			PreparedStatement prepareStatement = connection.prepareStatement(updateTransStatus);
			prepareStatement.setInt(1, status);
			prepareStatement.setInt(2, codec.findId(APP_ID, pId.getAppId()));
			prepareStatement.setInt(3, codec.findId(BUSINESS_CODE, pId.getBusCode()));
			prepareStatement.setLong(4, pId.getTrxId());
			prepareStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("updateExecuteFlagForSlaveTrx failed ",e);
		}
	}

}
