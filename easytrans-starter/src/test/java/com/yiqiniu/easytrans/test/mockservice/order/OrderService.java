package com.yiqiniu.easytrans.test.mockservice.order;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.protocol.msg.PublishResult;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingRequest;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingResponse;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.AfterMasterTransMethodResult;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.ExpressDeliverAfterTransMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayCascadeTccMethod.WalletPayCascadeTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodResult;

@Component
public class OrderService {
	
	public static final String EXCEPTION_TAG_BEFORE_COMMIT = "BeforeCommit";
	public static final String EXCEPTION_TAG_IN_THE_MIDDLE = "InTheMiddle";
	public static final String EXCEPTION_TAG_JUST_AFTER_START_EASY_TRANSACTION = "JustAfterStartEasyTrans";
	public static final String EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_SUCCESS_MASTER_TRANS = "InMiddleOfConsistentGuardianWithSuccessMasterTrans";
	public static final String EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_ROLLEDBACK_MASTER_TRANS = "InMiddleOfConsistentGuardianWithRolledBackMasterTrans";

	@Resource
	private TestUtil util;
	private long cascadeTrxFinishedSleepMills = 0;
	
	public static final String BUSINESS_CODE = "buySth";
	public static final String BUSINESS_CODE_CASCADE = "buySthCascade";
	
	private static Object NULL_OBJECT = new Object();
	private static ConcurrentHashMap<Class<?>,Object> notExecuteMap = new ConcurrentHashMap<Class<?>,Object>();
	private static boolean checkExecuteForTestCase(Class<?> checkBusiness){
		if(notExecuteMap.get(checkBusiness) != null){
			return false;
		}else{
			return true;
		}
	}
	
	public static void setNotExecuteBusiness(Class<?> requestClass){
		notExecuteMap.put(requestClass,NULL_OBJECT);
	}
	
	public static void clearNotExecuteSet(){
		notExecuteMap.clear();
	}
	
	private static ConcurrentHashMap<String,Object> throwExceptionSet = new  ConcurrentHashMap<String,Object>();
	public static void checkThrowException(String exceptionTag){
		if(throwExceptionSet.get(exceptionTag) != null){
			throw new UtProgramedException("exception set in UT:" + exceptionTag);
		}
	}
	
	public static class UtProgramedException extends RuntimeException{

		private static final long serialVersionUID = 1L;

		public UtProgramedException(String message) {
			super(message);
		}
		
	}

	public static void setExceptionTag(String exceptionTag){
		throwExceptionSet.put(exceptionTag,NULL_OBJECT);
	}
	
	public static void clearExceptionSet(){
		throwExceptionSet.clear();
	}
	
	
	

	public void setCascadeTrxFinishedSleepMills(long cascadeTrxFinishedSleepMills) {
		this.cascadeTrxFinishedSleepMills = cascadeTrxFinishedSleepMills;
	}

	@Resource
	private EasyTransFacade transaction;
	
	public int getUserOrderCount(int userId){
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID, BUSINESS_CODE, String.valueOf(userId));
		Integer queryForObject = jdbcTemplate.queryForObject("SELECT count(1) FROM `order` where user_id = ?;", Integer.class,userId);
		return queryForObject == null?0:queryForObject;
	}
	
	@Transactional("buySthTransactionManager")
	public void buySomethingCascading(int userId,long money){
		
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID, BUSINESS_CODE_CASCADE, String.valueOf(userId));
		Integer id = saveOrderRecord(jdbcTemplate,userId,money);

		transaction.startEasyTrans(BUSINESS_CODE_CASCADE, String.valueOf(id));
		
		/**
		 * 调用级联事务
		 */
		WalletPayCascadeTccMethodRequest deductRequest = new WalletPayCascadeTccMethodRequest();
		deductRequest.setUserId(userId);
		deductRequest.setPayAmount(money);
		Future<WalletPayTccMethodResult> execute = transaction.execute(deductRequest);
		
		
		try {
			//主动获取一下结果，触发真正的远程调用
			execute.get();
		} catch (InterruptedException | ExecutionException e1) {
			e1.printStackTrace();
		}
		
		//在事务结束后主动停止若干秒，用于等待远端事务缓存过期，以测试该场景
		if(cascadeTrxFinishedSleepMills > 0){
			try {
				Thread.sleep(cascadeTrxFinishedSleepMills);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	
	@Transactional("buySthTransactionManager")
	public Future<AfterMasterTransMethodResult> buySomething(int userId,long money){
		
		/**
		 * finish the local transaction first, in order for performance and generated of business id
		 * 
		 * 优先完成本地事务以 1. 提高性能（减少异常时回滚消耗）2. 生成事务内交易ID 
		 */
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID, BUSINESS_CODE, String.valueOf(userId));
		Integer id = saveOrderRecord(jdbcTemplate,userId,money);
		
		/**
		 * annotation the global transactionId, it is combined of appId + bussiness_code + id
		 * if this method did not call,the following calls of EasyTransFacade.execute will throw an exception
		 * 
		 * 声明全局事务ID，其由appId,业务代码，业务代码内ID构成
		 * 如果这个方法没有被调用，那么后续的EasyTransFacade.execute方法调用会抛异常
		 */
		transaction.startEasyTrans(BUSINESS_CODE, String.valueOf(id));
		checkThrowException(EXCEPTION_TAG_JUST_AFTER_START_EASY_TRANSACTION);
		
		/**
		 * call remote service to deduct money, it's a TCC service,
		 * framework will maintains the eventually constancy based on the final transaction status of method buySomething 
		 * 
		 * 调用远程服务扣除所需的钱,这个远程服务实现了TCC接口,
		 * 框架会根据buySomething方法的事务结果来维护远程服务的最终一致性
		 */
		WalletPayTccMethodRequest deductRequest = new WalletPayTccMethodRequest();
		deductRequest.setUserId(userId);
		deductRequest.setPayAmount(money/10);
		//return future for the benefits of performance enhance(batch write execute log and batch execute RPC)
		//返回future是为了能方便的优化性能(批量写日志及批量调用RPC)
		Future<WalletPayTccMethodResult> deductFuture = null;
		if(checkExecuteForTestCase(deductRequest.getClass())){
			/**
			 * 执行10遍，每次都扣十分之一钱，以测试相同方法在业务上调用多次的场景
			 * 因之前版本不支持同一事物内调用同一个方法多次，这里只是测试调用多次的场景，并无其他特殊含义
			 */
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
			deductFuture = transaction.execute(deductRequest);
		}
		

		
		/**
		 * call remote service to accounting, it's a compensable service
		 * framework will execute cancel method automatically if the transaction of buySomething() roll back
		 * 
		 * actually,use an queue message to trigger accounting will be more grace,
		 * but here just intends to show the use of compensable service, 
		 * the following business remote call may be not implement in the suitable service form,but it's for the same reason.
		 * 
		 * 
		 * 调用远程服务进行账务登记，账务服务是一个可补偿服务
		 * 当buySomething方法对应的事务回滚了，框架将会自动调用Cancel业务方法
		 * 
		 * 实际上，使用队列消息来触发记账会更为合适，
		 * 但是这里只是为了演示可补偿服务所以没使用队列消息，
		 * 接下来的一些业务代码的远程调用可能使用的服务形式不太合适，但都是为了展示框架的一些用法
		 */
		AccountingRequest accountingRequest = new AccountingRequest();
		accountingRequest.setAmount(money);
		accountingRequest.setUserId(userId);
		if(checkExecuteForTestCase(accountingRequest.getClass())){
			@SuppressWarnings("unused")
			Future<AccountingResponse> accountingFuture = transaction.execute(accountingRequest);
		}
		
		checkThrowException(EXCEPTION_TAG_IN_THE_MIDDLE);
		
		/**
		 * publish an message to trigger relative business handling,for example add user points, it's a reliable message
		 * the message will be sent after the transaction of buySomething() committed.
		 * Whether it's success consume depends on the implement of queue
		 * 
		 * 发布消息以触发相关的业务处理，例如增加积分。这是一个可靠的消息。
		 * 这个消息会在buySomething()的事务提交后，保证成功发布出去
		 * 但至于消息是否成功消费，这取决于Queue接口的具体实现
		 */
		OrderMessage orderMessage = new OrderMessage();
		orderMessage.setUserId(userId);
		orderMessage.setAmount(money);
		if(checkExecuteForTestCase(orderMessage.getClass())){
			@SuppressWarnings("unused")
			Future<PublishResult> reliableMessage = transaction.execute(orderMessage);
		}
		
		/**
		 * publish a Best Effort message,It's the same with reliable message but publish action is not guaranteed
		 * It do not need persist,so it's much faster then Reliable Message
		 * The relative service is notificationService
		 * 
		 * 发布一个最大努力交付的消息，其与可靠消息一样，除了其不保证消息一定被发布出去
		 * 最大努力交付消息不需要持久化，其将会比可靠消息速度更高
		 * 与本消息相关的服务是通知服务
		 */
		NotReliableOrderMessage notReliableMessage = new NotReliableOrderMessage();
		notReliableMessage.setUserId(userId);
		notReliableMessage.setAmount(money);
		if(checkExecuteForTestCase(notReliableMessage.getClass())){
			transaction.execute(notReliableMessage);
		}
		
		/**
		 * some actions is a asynchronous action and in most cases it can be done quickly
		 * and the product design is to return the result synchronize,so we have this.
		 * Execute method return an future object , 
		 * but The actually business will execute after buySomething() transaction commit.
		 * The future object can only get the result after the transaction commit.
		 * 
		 * 有一些操作本质上是一个异步操作，但是其在大多数情况下可以迅速执行完毕，并且产品设计要求同步返回结果，所以框架提供了这一种形式的服务
		 * Execute方法在执行后返回一个Future对象
		 * 但这个实际执行的操作将会在buySomething()的事务提交后执行.
		 * Future对象只能在执行提交后才能get到执行结果
		 */
		ExpressDeliverAfterTransMethodRequest expressRequest = new ExpressDeliverAfterTransMethodRequest();
		expressRequest.setPayAmount(money);
		expressRequest.setUserId(userId);
		Future<AfterMasterTransMethodResult> expressResult = null;
		if(checkExecuteForTestCase(expressRequest.getClass())){
			expressResult = transaction.execute(expressRequest);
		}
		
		/**
		 * the arbitrarily  get method will trigger the logs write and RPC method's call
		 * if there is an exception result in the future objects, the buySomething() business will be roll back before commit by framework
		 * 
		 * 调用任意的get方法都会触发批量写日志及RPC远程调用
		 * 只要有任意的get方法的返回值是exception,buySomething()事务就会被框架在提交前整体回滚
		 */
		try {
			if(deductFuture != null){
				WalletPayTccMethodResult walletPayTccMethodResult = deductFuture.get();
				System.out.println(walletPayTccMethodResult.toString());
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter(){
			@Override
			public void beforeCommit(boolean readOnly) {
				checkThrowException(EXCEPTION_TAG_BEFORE_COMMIT);
			}
		});
		
		return expressResult;
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
