package com.yiqiniu.easytrans.log.impl.database;

import java.util.Date;
import java.util.List;

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
import org.springframework.util.StringUtils;

import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ByteFormIdCodec;

public class DataBaseTransactionLogWritterImpl implements TransactionLogWritter {
	
	private String cleanLog = "delete from trans_log_detail where create_time < ? and trans_log_id not in (select trans_log_id from trans_log_unfinished);";
	private String deleteUnfinishedTag = "DELETE FROM trans_log_unfinished WHERE trans_log_id = ?;";
	private String insertTransDetail = "INSERT INTO `trans_log_detail` (`log_detail_id`, `trans_log_id`, `log_detail`, `create_time`) VALUES (NULL, ?, ?, ?);";
	private String insertUnfinished = "insert into `trans_log_unfinished` VALUES(?,?) on DUPLICATE KEY UPDATE create_time = create_time;";

	private DataSource dataSource;
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private DataSourceTransactionManager transactionManager;
	private TransactionTemplate transactionTemplate;
	private ByteFormIdCodec idCodec;
	private ObjectSerializer objectSerializer;
	
	public DataBaseTransactionLogWritterImpl(ObjectSerializer objectSerializer,DataSource dataSource, ByteFormIdCodec idCodec,String tablePrefix) {
		super();
		this.objectSerializer = objectSerializer;
		this.idCodec = idCodec;
		this.dataSource = dataSource;
		transactionManager = new DataSourceTransactionManager(dataSource);
		transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
		
		if(!StringUtils.isEmpty(tablePrefix)) {
			tablePrefix = tablePrefix.trim();
			
			cleanLog = cleanLog.replace("trans_log_detail", tablePrefix + "trans_log_detail");
			cleanLog = cleanLog.replace("trans_log_unfinished", tablePrefix + "trans_log_unfinished");
			deleteUnfinishedTag = deleteUnfinishedTag.replace("trans_log_unfinished", tablePrefix + "trans_log_unfinished");
			insertTransDetail = insertTransDetail.replace("trans_log_detail", tablePrefix + "trans_log_detail");
			insertUnfinished = insertUnfinished.replace("trans_log_unfinished", tablePrefix + "trans_log_unfinished");
		}
	
	}

	private JdbcTemplate jdbcTemplate;
	private JdbcTemplate getJdbcTemplate(){
		if(jdbcTemplate == null){
			jdbcTemplate = new JdbcTemplate(dataSource);
		}
		return jdbcTemplate;
	}
	
	@Override
	public void appendTransLog(final String appId, final String busCode, final long trxId,
			final List<Content> newOrderedContent, final boolean finished) {
		
		transactionTemplate.execute(new TransactionCallback<Object>(){
			@Override
			public Object doInTransaction(TransactionStatus status) {
				JdbcTemplate localJdbcTemplate = getJdbcTemplate();
				
				//unfinished tag
				byte[] transIdByteForm = idCodec.getTransIdByte(new TransactionId(appId, busCode, trxId));
				localJdbcTemplate.update(insertUnfinished, transIdByteForm,new Date());
				
				if(newOrderedContent != null && newOrderedContent.size() != 0){
					//concrete log
					int logUpdateConut = localJdbcTemplate.update(insertTransDetail,
							transIdByteForm,
							objectSerializer.serialization(newOrderedContent),
							new Date()
							);
					if(logUpdateConut != 1){
						throw new RuntimeException("write log error!");
					}
					
					if(LOG.isDebugEnabled()){
						LOG.debug(newOrderedContent.toString());
					}
				}
				
				if(finished){
					//remove unfinished tag
					localJdbcTemplate.update(deleteUnfinishedTag, transIdByteForm);
				}
				
				return null;
			}
			
		});
	}


	public void cleanFinishedLogs(String appId,final Date cleanBefore) {
		transactionTemplate.execute(new TransactionCallback<Object>(){
			@Override
			public Object doInTransaction(TransactionStatus status) {
				JdbcTemplate localJdbcTemplate = getJdbcTemplate();
				localJdbcTemplate.update(cleanLog,cleanBefore);
				return null;
			}
		});
	}
	
}
