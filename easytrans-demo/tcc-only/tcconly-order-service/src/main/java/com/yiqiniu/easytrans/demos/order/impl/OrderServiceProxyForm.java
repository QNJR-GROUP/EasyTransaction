package com.yiqiniu.easytrans.demos.order.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yiqiniu.easytrans.demos.order.remoteservice.WalletPayMoneyService;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;

@Component
public class OrderServiceProxyForm {
	

	@Resource
	private WalletPayMoneyService payService;
	@Resource
	private JdbcTemplate jdbcTemplate;
	
	
	@Transactional
	public String buySomething(int userId,long money){
		
		int id = saveOrderRecord(jdbcTemplate,userId,money);
		WalletPayRequestVO request = new WalletPayRequestVO();
		request.setUserId(userId);
		request.setPayAmount(money);
		WalletPayResponseVO pay = payService.pay(request);

		return "id:" + id + " freeze:" + pay.getFreezeAmount();
	}
	
	
	private Integer saveOrderRecord(JdbcTemplate jdbcTemplate, final int userId, final long money) {
		
		final String INSERT_SQL = "INSERT INTO `order` (`order_id`, `user_id`, `money`, `create_time`) VALUES (NULL, ?, ?, ?);";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(
		    new PreparedStatementCreator() {
		    	@Override
		        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
		            PreparedStatement ps =
		                connection.prepareStatement(INSERT_SQL, new String[] {"id"});
		            ps.setInt(1, userId);
		            ps.setLong(2, money);
		            ps.setDate(3, new Date(System.currentTimeMillis()));
		            return ps;
		        }
		    },
		    keyHolder);
		
		return keyHolder.getKey().intValue();
	}
	
	
}
