package com.yiqiniu.easytrans.log.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class DataBaseTransactionLogReaderImpl implements TransactionLogReader {
	
	private DataSource dataSource;
	
	@Resource
	private ObjectSerializer serializer;
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
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
		
		List<DataBaseTransactionLogDetailDo> query;
		
		List<String> transIdList = null;
		if(locationId != null){
			String transIdLocation = DataBaseTransactionLogDetailDo.getTransId(locationId.getAppId(), locationId.getBusCode(), locationId.getTrxId());
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
		query = namedTemplate.query("select * from trans_log_detail where trans_log_id in (:ids)  order by trans_log_id,log_detail_id;", paramSource,new BeanPropertyRowMapper<DataBaseTransactionLogDetailDo>(DataBaseTransactionLogDetailDo.class));
			
		
		List<LogCollection> result = new ArrayList<LogCollection>();
		List<DataBaseTransactionLogDetailDo> currentDoList = new ArrayList<DataBaseTransactionLogDetailDo>();
		List<Content> currentContentList = new ArrayList<Content>();
		String currentId = null;
		for(DataBaseTransactionLogDetailDo detailDo:query){
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
	private List<Content> deserializer(DataBaseTransactionLogDetailDo detailDo) {
		return serializer.deserialize(detailDo.getLogDetail());
	}
	
	private void addToResult(List<LogCollection> result,
			List<DataBaseTransactionLogDetailDo> currentDoList,
			List<Content> currentContentList) {
		if(currentDoList.size() != 0){
			DataBaseTransactionLogDetailDo first = currentDoList.get(0);
			String[] splitTransId = DataBaseTransactionLogDetailDo.getSplitTransId(first.getTransLogId());
			result.add(new LogCollection(splitTransId[0], splitTransId[1], splitTransId[2], currentContentList, first.getCreateTime()));
		}
	}
	
	
}
