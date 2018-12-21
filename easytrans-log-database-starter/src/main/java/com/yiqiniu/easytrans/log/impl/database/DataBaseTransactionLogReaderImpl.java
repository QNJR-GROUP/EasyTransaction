package com.yiqiniu.easytrans.log.impl.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ByteFormIdCodec;



public class DataBaseTransactionLogReaderImpl implements TransactionLogReader {
	
	
	
	private String selectTransDetailsByIds = "select * from trans_log_detail where trans_log_id in (:ids)  order by trans_log_id,log_detail_id;";
	private String selectUnfinishedTransWithPos = "select trans_log_id from trans_log_unfinished where trans_log_id <= ? and trans_log_id >= ? and create_time <= ? ORDER BY trans_log_id LIMIT ?";
	private String selectUnfinishedTransWithoutPos = "select trans_log_id from trans_log_unfinished where trans_log_id <= ? and trans_log_id >= ? and create_time <= ? and trans_log_id > ? ORDER BY trans_log_id LIMIT ?";
	
	
	public DataBaseTransactionLogReaderImpl(String appId, ObjectSerializer serializer,DataSource dataSource, ByteFormIdCodec idCodec, String tablePrefix) {
		super();
		this.serializer = serializer;
		this.dataSource = dataSource;
		this.appId = appId;
		this.idCodec = idCodec;
		
		if(!StringUtils.isEmpty(tablePrefix)) {
			tablePrefix = tablePrefix.trim();
			selectTransDetailsByIds = selectTransDetailsByIds.replace("trans_log_detail", tablePrefix + "trans_log_detail");
			selectUnfinishedTransWithPos = selectUnfinishedTransWithPos.replace("trans_log_unfinished", tablePrefix + "trans_log_unfinished");
			selectUnfinishedTransWithoutPos = selectUnfinishedTransWithoutPos.replace("trans_log_unfinished", tablePrefix + "trans_log_unfinished");

		}
		
	}
	
	private DataSource dataSource;
	private ObjectSerializer serializer;
	private String appId;
	private ByteFormIdCodec idCodec;

	
	private JdbcTemplate jdbcTemplate;
	private JdbcTemplate getJdbcTemplate(){
		if(jdbcTemplate == null){
			jdbcTemplate = new JdbcTemplate(dataSource);
		}
		return jdbcTemplate;
	}
	@Override
	public List<LogCollection> getUnfinishedLogs(LogCollection locationId,
			int pageSize, Date createTimeCeiling) {
		
		JdbcTemplate localJdbcTemplate = getJdbcTemplate();
		
		List<DataBaseTransactionLogDetail> query;
		
		List<byte[]> transIdList = null;
		if(locationId != null){
			byte[] transIdLocation = idCodec.getTransIdByte(new TransactionId(locationId.getAppId(), locationId.getBusCode(), locationId.getTrxId()));
			transIdList = localJdbcTemplate.queryForList(selectUnfinishedTransWithoutPos, new Object[]{idCodec.getAppIdCeil(appId), idCodec.getAppIdFloor(appId), createTimeCeiling,transIdLocation,pageSize},byte[].class);
		}else{
			transIdList = localJdbcTemplate.queryForList(selectUnfinishedTransWithPos, new Object[]{idCodec.getAppIdCeil(appId), idCodec.getAppIdFloor(appId), createTimeCeiling,pageSize},byte[].class);
		}
		
		if(transIdList == null || transIdList.size() ==0){
			return new ArrayList<LogCollection>();
		}
		
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(localJdbcTemplate);
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("ids", transIdList);
		query = namedTemplate.query(selectTransDetailsByIds, paramSource,new BeanPropertyRowMapper<DataBaseTransactionLogDetail>(DataBaseTransactionLogDetail.class));
			
		
		List<LogCollection> result = new ArrayList<LogCollection>();
		List<DataBaseTransactionLogDetail> currentDoList = new ArrayList<DataBaseTransactionLogDetail>();
		List<Content> currentContentList = new ArrayList<Content>();
		byte[] currentId = null;
		for(DataBaseTransactionLogDetail detailDo:query){
			if(!Arrays.equals(detailDo.getTransLogId(), currentId)){
				addToResult(result, currentDoList, currentContentList);
				currentContentList.clear();
				currentDoList.clear();
				currentId = detailDo.getTransLogId();
			}
			
			currentDoList.add(detailDo);
			currentContentList.addAll(deserializer(detailDo));
		}
		addToResult(result, currentDoList, currentContentList);
		
		return result;
	}
	private List<Content> deserializer(DataBaseTransactionLogDetail detailDo) {
		return serializer.deserialize(detailDo.getLogDetail());
	}
	
	private void addToResult(List<LogCollection> result,
			List<DataBaseTransactionLogDetail> currentDoList,
			List<Content> currentContentList) {
		if (currentDoList.size() != 0) {
			DataBaseTransactionLogDetail first = currentDoList.get(0);
			TransactionId splitTransId = idCodec.getTransIdFromByte(first.getTransLogId());
			result.add(new LogCollection(splitTransId.getAppId(),
					splitTransId.getBusCode(), 
					splitTransId.getTrxId(),
					new ArrayList<Content>(currentContentList), first.getCreateTime()));
		}
	}
		
}
