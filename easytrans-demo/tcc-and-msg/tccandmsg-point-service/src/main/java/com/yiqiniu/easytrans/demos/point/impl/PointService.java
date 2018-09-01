package com.yiqiniu.easytrans.demos.point.impl;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yiqiniu.easytrans.demos.order.api.vo.OrderFinishedMessage;

@Component
public class PointService {
	
	@Resource
	private JdbcTemplate jdbcTemplate;
	
	@Transactional
	public void addPointForBuying(OrderFinishedMessage msg){
		int update = jdbcTemplate.update("update `point` set point = point + ? where user_id = ?;", 
				msg.getOrderAmt(),msg.getUserId());
		
		if(update != 1){
			throw new RuntimeException("can not find specific user id!");
		}
	}
	

}
