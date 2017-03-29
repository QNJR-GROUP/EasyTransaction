package com.yiqiniu.easytrans.log.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class DataBaseTransactionLogWritterImpl implements TransactionLogWritter {
	
	private DataSource dataSource;
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private DataSourceTransactionManager transactionManager;
	TransactionTemplate transactionTemplate;
	
	@Resource
	private ObjectSerializer objectSerializer;
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		transactionManager = new DataSourceTransactionManager(dataSource);
		transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
	}
	
	private JdbcTemplate jdbcTemplate;
	private JdbcTemplate getJdbcTemplate(){
		if(jdbcTemplate == null){
			jdbcTemplate = new JdbcTemplate(dataSource);
		}
		return jdbcTemplate;
	}
	
	@Override
	public void appendTransLog(final String appId, final String busCode, final String trxId,
			final List<Content> newOrderedContent, final boolean finished) {
		
		transactionTemplate.execute(new TransactionCallback<Object>(){
			@Override
			public Object doInTransaction(TransactionStatus status) {
				JdbcTemplate localJdbcTemplate = getJdbcTemplate();
				
				//unfinished tag
				localJdbcTemplate.update("insert into `trans_log_unfinished` VALUES(?,?) on DUPLICATE KEY UPDATE create_time = create_time;", DataBaseTransactionLogDetailDo.getTransId(appId, busCode, trxId),new Date());
				
				//concrete log
				int logUpdateConut = localJdbcTemplate.update("INSERT INTO `trans_log_detail` (`log_detail_id`, `trans_log_id`, `log_detail`, `create_time`) VALUES (NULL, ?, ?, ?);",
						DataBaseTransactionLogDetailDo.getTransId(appId, busCode, trxId),
						objectSerializer.serialization(newOrderedContent),
						new Date()
						);
				if(logUpdateConut != 1){
					throw new RuntimeException("write log error!");
				}
				
				if(LOG.isDebugEnabled()){
					LOG.debug(newOrderedContent.toString());
				}
				
				if(finished){
					//remove unfinished tag
					localJdbcTemplate.update("DELETE FROM trans_log_unfinished WHERE trans_log_id = ?;", DataBaseTransactionLogDetailDo.getTransId(appId,busCode,trxId));
				}
				
				return null;
			}
			
		});
	}


	@Override
	public void cleanFinishedLogs(String appId,final Date cleanBefore) {
		transactionTemplate.execute(new TransactionCallback<Object>(){
			@Override
			public Object doInTransaction(TransactionStatus status) {
				JdbcTemplate localJdbcTemplate = getJdbcTemplate();
				localJdbcTemplate.update("delete from trans_log_detail where create_time < ? and trans_log_id not in (select trans_log_id from trans_log_unfinished);",cleanBefore);
				return null;
			}
		});
	}
	
}
