package com.yiqiniu.easytrans.log.impl.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.yiqiniu.easytrans.core.EasyTransStaticHelper;
import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class DataBaseTransactionLogReaderImpl implements TransactionLogReader {
	
	public DataBaseTransactionLogReaderImpl(ObjectSerializer serializer,DataSource dataSource) {
		super();
		this.serializer = serializer;
		this.dataSource = dataSource;
	}
	
	private DataSource dataSource;
	
	private ObjectSerializer serializer;
	
	private JdbcTemplate jdbcTemplate;
	private JdbcTemplate getJdbcTemplate(){
		if(jdbcTemplate == null){
			jdbcTemplate = new JdbcTemplate(dataSource);
		}
		return jdbcTemplate;
	}
	@Override
	public List<LogCollection> getUnfinishedLogs(LogCollection locationId,
			int pageSize, Date createTimeFloor) {
		
		JdbcTemplate localJdbcTemplate = getJdbcTemplate();
		
		List<DataBaseTransactionLogDetail> query;
		
		List<String> transIdList = null;
		if(locationId != null){
			String transIdLocation = EasyTransStaticHelper.getTransId(locationId.getAppId(), locationId.getBusCode(), locationId.getTrxId());
			transIdList = localJdbcTemplate.queryForList("select trans_log_id from trans_log_unfinished where create_time <= ? and trans_log_id > ? ORDER BY trans_log_id LIMIT ?", new Object[]{createTimeFloor,transIdLocation,pageSize},String.class);
		}else{
			transIdList = localJdbcTemplate.queryForList("select trans_log_id from trans_log_unfinished where create_time <= ? ORDER BY trans_log_id LIMIT ?", new Object[]{createTimeFloor,pageSize},String.class);
		}
		
		if(transIdList == null || transIdList.size() ==0){
			return new ArrayList<LogCollection>();
		}
		
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(localJdbcTemplate);
		MapSqlParameterSource paramSource = new MapSqlParameterSource();
		paramSource.addValue("ids", transIdList);
		query = namedTemplate.query("select * from trans_log_detail where trans_log_id in (:ids)  order by trans_log_id,log_detail_id;", paramSource,new BeanPropertyRowMapper<DataBaseTransactionLogDetail>(DataBaseTransactionLogDetail.class));
			
		
		List<LogCollection> result = new ArrayList<LogCollection>();
		List<DataBaseTransactionLogDetail> currentDoList = new ArrayList<DataBaseTransactionLogDetail>();
		List<Content> currentContentList = new ArrayList<Content>();
		String currentId = null;
		for(DataBaseTransactionLogDetail detailDo:query){
			if(!detailDo.getTransLogId().equals(currentId)){
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
		if(currentDoList.size() != 0){
			DataBaseTransactionLogDetail first = currentDoList.get(0);
			String[] splitTransId = EasyTransStaticHelper.getSplitTransId(first.getTransLogId());
			result.add(new LogCollection(splitTransId[0], splitTransId[1], splitTransId[2], new ArrayList<Content>(currentContentList), first.getCreateTime()));
		}
	}
	
	
}
