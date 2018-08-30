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

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.demos.order.impl.OrderStatusHandler.UpdateOrderStatus;
import com.yiqiniu.easytrans.demos.wallet.api.vo.WalletPayVO.WalletPayRequestVO;

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
		Integer id = saveOrderRecord(userId,money);
		
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
		 * call remote service after local transaction finished to deduct money, it's a SAGA-TCC service,
		 * framework will maintains the eventually constancy based on the final transaction status
		 * if you think introducing object transaction(EasyTransFacade) is an unacceptable coupling
		 * then you can refer to another demo(interfacecall) in the demos directory, it will show you how to execute transaction by user defined interface
		 * 
		 * 在本地事务结束后，调用远程服务扣除所需的钱,这个远程服务实现了SAGA-TCC接口,
		 * 框架会自动维护全局事务的最终一致性
		 * 如果你认为引入transaction（EasyTransFacde）是一个无法接受的耦合
		 * 那么你可以参考在demos目录下另外一个样例(interfacecall)，它会告诉你如何用用户自定义的接口来执行远程事务
		 */
		WalletPayRequestVO deductRequest = new WalletPayRequestVO();
		deductRequest.setUserId(userId);
		deductRequest.setPayAmount(money);
		//deductRequest.setOrderId(id);
		transaction.execute(deductRequest);
		
		
		/**
		 * you can add more types of transaction calls here, e.g. TCC,reliable message, another SAGA-TCC and so on
		 * framework will maintains the eventually consistent 
		 * 
		 * 你可以额外加入其它类型的事务，如TCC,可靠消息，另外一个SAGA-TCC等等
		 * 框架会维护全局事务的最终一致性
		 */
		
		/**
		 * finally,use a SAGA-TCC request to asyn update the status of order，
		 * framework will execute confirm or cancel based on the final transction status
		 * it's the difference between SAGA-TCC and other types of transactions
		 * if SAGA-TCC is not included in the global-transaction, records written in local transaction will roll back
		 * but in SAGA model master transaction will split in two transactions, so we have to handle the cleanup/confirm job manually
		 * 
		 * 最后，使用SAGA-TCC来异步更新ORDER的状态
		 * 框架将会根据全局事务状态来执行confirm或者cancel方法
		 * 这个是SAGA-TCC与其他事务类型的明显区别
		 * 当SAGA-TCC类型事务没有加入到全局事务时，本方法执行的记录都会自动回滚掉
		 * 但SAGA模式的主控事务会拆分成两个事务，所以，我们需要手工写代码处理清理操作或者确认操作
		 */
		UpdateOrderStatus updateOrderStatus = new UpdateOrderStatus();
		updateOrderStatus.setOrderId(id);
		transaction.execute(updateOrderStatus);

		return id;
	}
	
	
	private Integer saveOrderRecord(final int userId, final long money) {
		
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
	
	
	public void cancelOrder(int orderId) {
		
		/**
		 * usually we will only update a status of order instead of delete
		 * but i did not define a status in the demo schema,so just delete instead
		 * 
		 * 通常不会直接删除之前创建记录，仅仅只是更新记录的状态
		 * 但是在测试的表结构里没有定义status字段，因此在样例代码里直接删除
		 */
		final String INSERT_SQL = "DELETE FROM `order` WHERE `order_id`= ?";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(
		    new PreparedStatementCreator() {
		    	@Override
		        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
		            PreparedStatement ps =
		                connection.prepareStatement(INSERT_SQL, new String[] {"id"});
		            ps.setInt(1, orderId);
		            return ps;
		        }
		    },
		    keyHolder);
	}
	
	public void confirmOrder(int orderId) {
		
		/**
		 * usually we will update the status of the order
		 * but i did not define a status in the demo schema,so just do nothing
		 * 
		 * 通常在确认操作里会更新记录状态
		 * 但是在测试的表结构里没有定义status字段，因此在测试代码里就啥都不做好了
		 */
	}
	
	
}
