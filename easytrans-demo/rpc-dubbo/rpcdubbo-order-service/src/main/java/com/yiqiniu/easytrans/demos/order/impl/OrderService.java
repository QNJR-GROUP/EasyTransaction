package com.yiqiniu.easytrans.demos.order.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayResponseVO;

@Component
public class OrderService {
	
	public static final String BUSINESS_CODE = "buySth";


	@Resource
	private EasyTransFacade transaction;
	@Resource
	private JdbcTemplate jdbcTemplate;
	
	
	@Transactional
	public int buySomething(int userId,long money){
		
		/**
		 * finish the local transaction first, in order for performance and generated of business id
		 * 
		 * 优先完成本地事务以 1. 提高性能（减少异常时回滚消耗）2. 生成事务内交易ID 
		 */
		Integer id = saveOrderRecord(jdbcTemplate,userId,money);
		
		/**
		 * annotation the global transactionId, it is combined of appId + bussiness_code + id
		 * it can be omit,then framework will use "default" as businessCode, and will generate an id
		 * but it will make us harder to associate an global transaction to an concrete business
		 * 
		 * 声明全局事务ID，其由appId,业务代码，业务代码内ID构成
		 * 本代码可以省略，框架会自动生成ID及使用一个默认的业务代码
		 * 但这样的话，会使得我们难以把全局事务ID与一个具体的事务关联起来
		 */
		transaction.startEasyTrans(BUSINESS_CODE, id);
		
		/**
		 * call remote service to deduct money, it's a TCC service,
		 * framework will maintains the eventually constancy based on the final transaction status of method buySomething 
		 * if you think introducing object transaction(EasyTransFacade) is an unacceptable coupling
		 * then you can refer to another demo(interfacecall) in the demos directory, it will show you how to execute transaction by user defined interface
		 * 
		 * 调用远程服务扣除所需的钱,这个远程服务实现了TCC接口,
		 * 框架会根据buySomething方法的事务结果来维护远程服务的最终一致性
		 * 如果你认为引入transaction（EasyTransFacde）是一个无法接受的耦合
		 * 那么你可以参考在demos目录下另外一个样例(interfacecall)，它会告诉你如何用用户自定义的接口来执行远程事务
		 */
		WalletPayRequestVO deductRequest = new WalletPayRequestVO();
		deductRequest.setUserId(userId);
		deductRequest.setPayAmount(money);
		/**
		 * return future for the benefits of performance enhance(batch write execute log and batch execute RPC)
		 * 返回future是为了能方便的优化性能(批量写日志及批量调用RPC)
		 */
		@SuppressWarnings("unused")
		Future<WalletPayResponseVO> deductFuture = transaction.execute(deductRequest);
		
		/**
		 * you can add more types of transaction calls here, e.g. TCC,reliable message, SAGA-TCC and so on
		 * framework will maintains the eventually consistent 
		 * 
		 * 你可以额外加入其它类型的事务，如TCC,可靠消息，SAGA-TCC等等
		 * 框架会维护全局事务的最终一致性
		 */
		
		
		/**
		 * we can get remote service result to determine whether to commit this transaction
		 * 
		 * 可以获取远程返回的结果用以判断是继续往下走 还是 抛异常结束
		 * 
		 * deductFuture.get(); 
		 */

		return id;
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
