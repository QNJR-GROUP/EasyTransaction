package com.yiqiniu.easytrans.test.mockservice.point;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.order.OrderMessage;
import com.yiqiniu.easytrans.util.ReflectUtil;

@Component
public class PointService {
	
	
	@Resource
	private TestUtil util;
	
	@Value("spring.application.name")
	private String applicationName;
	
	public void addPointForBuying(OrderMessage msg){

		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(msg.getClass());
		
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(applicationName,businessIdentifer.busCode(),msg);
		int update = jdbcTemplate.update("update `point` set point = point + ? where user_id = ?;", 
				msg.getAmount(),msg.getUserId());
		
		if(update != 1){
			throw new RuntimeException("can not find specific user id!");
		}
	}
	
	
	public int getUserPoint(int userId){
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(applicationName,OrderMessage.BUSINESS_CODE,(OrderMessage)null);
		Integer queryForObject = jdbcTemplate.queryForObject("select point from point where user_id = ?", Integer.class, userId);
		return queryForObject == null?0:queryForObject;
	}
}
