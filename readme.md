# English
(English is not my mother tongue, can someone help me to review the text below?)

## The origin

This framework is inspired by a PPT <Distribute transation handling in a large scale SOA system>(<大规模SOA系统的分布式事务处理>) written by Cheng Li who works in Alipay


This framework aims to solve the problem in our company that have repeatedly designed intermediate states, idempotent implementations, and retry logic for each distributed transaction scenario.


With this framework, we can solve all the distributed transaction scenarios that have been found, reduce the design and development workload, improve the development efficiency, and uniformly ensure the reliability of transaction implementation.


Characteristic:

* a framework composited all types of transaction patterns.
* multiple pattern of transactions can be used together.
* High performance.
	* most business system bottlenecks are in the business database, 
	* and the framework's extra consumption to business database is a 25-byte row(if the framework's idempotent implementation is not enabled)
* Optional idempotent implementations and calling sequence guarantee implement
	* it will greatly reduce business development consumption
	* but when enabled, an idempotent control row will write into the business database
* Can be completely decoupled in business code, no framework class will introduced in businese code(but not configuration code)
* support nested transactions.
* Without additional deployment for coordinators, coordinators are in the service APP
	* you can also split coordinators out of service APP too
* Distributed transaction ID can be associated with businese ID, businese type, and APPID, which is convenient for monitoring the execution of distributed transactions of various services.



## Distributed transaction scenario and corresponding implementation of framework

Distributed business scenario

* no distributed transactions.
	* most commonly used
	* top priority
* use the message queue to achieve Eventually consistent
	* applicable to the business that can decide the global transaction status(commit/rollback) internally(with out other service)
	* commonly used
	* if you must interact with other service's write interfaces, give priority to use.
	* based on whether it is guaranteed to be delivered to subscribers, divided into reliable information and best effort delivery message.
* compensation pattern
	* applicable to businesses that require remote execution results to determine global-transaction status, and remote execution is able to make compensation.
	* common
	* Priority is given to compensation-based eventual consistency transactions if there's unsolvable problems with message queues are used
* TCC pattern
	* applicable to businesses that require remote execution results to determine global-transaction status, and remote execution is unable to make compensation.
	* the least common.
	* the final solution encompasses all scenarios that must be implemented using 2PC. 
	* The maximum amount of coding and the maximum consumption of performance,it should be avoid using as far as possible.
* SAGA pattern
	* it's similar to compensation pattern.
	* the difference between compensation pattern and SAGA is that slave/sub transactions are not executed during the local transaction of master
	* SAGA executed asynchronously through the queue, thus reducing the lock time of master transaction records
	* When a lot of synchronous calls(e.g. TCC,Compensation) are in master transacion, SAGA can be used instead of synchronous calls for performance
	* when using saga,there's something to be pay attention(just go and see demo), because the transaction of the master will be split into two.
	* The SAGA implemented in this framework is not a traditional SAGA. 
	* The difference between traditional SAGA and this SAGA can be analogized to the relationship between traditional compensation and TCC.
	* And the framework uses RPC instead of queues to implement SAGA for reasons that can be seen in the SAGA-TCC DEMO

### The corresponding implement and basic principles of the framework

The framework implements all kinds of transaction patterns mentioned above and provides a unified and easy to use interface. The following section introduced the basic principles.

#### without distribute transaction
just as the tradition local transaction, EastyTransaction totally not intervention

#### Other distribute transaction

The core dependency of the framework is Spring's TransactionSynchronization class. Easytransaction can be used as long as the TransactionManager inherits from AbstractPlatformTransactionManager (basically all the TransactionManager inherits from this implementation). In addition, after version 1.0.0, the framework uses SPRING BOOT configuration capabilities and JDK8 features, so SPRING BOOT and JDK8 is also a must option.


For distributed transactions, the framework hook the corresponding framework operations into TransactionSynchronization before calling the remote transaction method, e.g.:

* when using reliable messages, the framework will send messages after the golobal-transaction status conifrms, ensuring that the messages can be seen externally after global-transaction status confirms, lighten the burden of coding sending-acknowledging-detecting pattern
* when Using TCC, the framework calls Confirm or Cancel depending on the golobal-transaction status


The framework has background threads responsible for CRASH recovery (e.g. to execute confirm or rollback in TCC) based on "write logs prior to the execution of a distributed service invocation,so that we can tell whether the remote service may have been invoked" and "a framework record submitted with the transaction to determine the global-transaction status" 

The framework also has an  (optional) implementation of idempotency, which ensures that business methods are logically executed only once (it is possible to execute multiple times, but methods executed multiple times are rolled back, so business programs need to control their idempotency when it comes to non-rollbackable external resources)

The framework also handles method call orders, for example, in compensation pattern:
* If the original operation is not invoked(or delay by network), the corresponding compensation operation written by user will not be invoked(but framework will mark that compensation has already executed)
* If compensation methods has marked executed, the (delayed) orignal operation whether it has been executed or not, will not be invoked


The results of all remote calls are returned in the form of a Future object, which gives the framework room for performance optimization, and all logs will not be written until trying to obtain first result. Once a business program attempts to get execution results, it writes the log in bulk and subsequently calls the remote method concurrently.

The framework will check whether there's an exception in remote-calls before COMMIT. Once there's a remote method throws an Exception, the framework will roll back the business before commit. This ensures the simplicity of the programming model and facilitates the correct and understandable code.



## coding introduction

### code
#### Business initiator
For business initiators, the framework exposes only one interface.

    public interface EasyTransFacade {
	/**
	 * start easy Transaction
	 * @param busCode appId+busCode+trxId is a global unique id,no matter this transaction commit or roll back
	 * @param trxId see busCode
	 */
	void startEasyTrans(String busCode, String trxId);

	/**
	 * execute remote transaction
	 * @param params
	 * @return
	 */
	<P extends EasyTransRequest<R, E>, E extends EasyTransExecutor, R extends Serializable> Future<R> execute(P params);
    }

the remote method can be invoked directly without considering the specific type of distributed transaction and subsequent processing,as following:

	@Transactional
	public int buySomething(int userId,long money){
		
		/**
		 * finish the local transaction first, in order for performance and generated of business id
		 */
		Integer id = saveOrderRecord(jdbcTemplate,userId,money);
		
		/**
		 * annotation the global transactionId, it is combined of appId + bussiness_code + id
		 * this line of code can be omit,then framework will use "default" as businessCode, and will generate an id
		 * but it will make us harder to associate an global transaction to an concrete business
		 */
		transaction.startEasyTrans(BUSINESS_CODE, id);
		
		/**
		 * call remote service to deduct money, it's a TCC service,
		 * framework will maintains the eventually constancy based on the final transaction status of method buySomething 
		 * if you think introducing object transaction(EasyTransFacade) is an unacceptable coupling
		 * then you can refer to another demo(interfacecall) in the demos directory, it will show you how to execute transaction by user defined interface
		 */
		WalletPayRequestVO deductRequest = new WalletPayRequestVO();
		deductRequest.setUserId(userId);
		deductRequest.setPayAmount(money);
		
		/**
		 * return future for the benefits of performance enhance(batch write execute log and batch execute RPC)
		 */
		Future<WalletPayResponseVO> deductFuture = transaction.execute(deductRequest);
		
		
		/**
		 * publish a message when this global-transaction is confirm
		 * so the other services subscribe for this event can receive this message
		 */
		OrderFinishedMessage orderFinishedMsg = new OrderFinishedMessage();
		orderFinishedMsg.setUserId(userId);
		orderFinishedMsg.setOrderAmt(money);
		transaction.execute(orderFinishedMsg);
		
		/**
		 * you can add more types of transaction calls here, e.g. SAGA-TCC、Compensation and so on
		 * framework will maintains the eventually consistent 
		 */
		
		/**
		 * we can get remote service result to determine whether to commit this transaction 
		 * 
		 * deductFuture.get(); 
		 */
		return id;
	}

#### Service Provider
For service providers, the corresponding interface is implemented and registered to the Bean container of Spring.

For example in TCC，it needs to implements TccMethod：

    @Component
    public class WalletPayTccMethod implements TccMethod<WalletPayTccMethodRequest, WalletPayTccMethodResult>{

	@Resource
	private WalletService wlletService;

	@Override
	public WalletPayTccMethodResult doTry(WalletPayTccMethodRequest param) {
		return wlletService.doTryPay(param);
	}

	@Override
	public void doConfirm(WalletPayTccMethodRequest param) {
		wlletService.doConfirmPay(param);
	}

	@Override
	public void doCancel(WalletPayTccMethodRequest param) {
		wlletService.doCancelPay(param);
	}
    }

WalletPayTccMethodRequest is the request parameter, which is POJO inherited from the EasyTransRequest class, And it needs to add BusinessIdentifer annotations to tell the framework what's the AppID and  business code corresponding to this request:

	@BusinessIdentifer(appId=Constant.APPID,busCode=METHOD_NAME)
	public class WalletPayTccMethodRequest implements TccTransRequest<WalletPayTccMethodResult>{
		private static final long serialVersionUID = 1L;
		
		private Integer userId;
		
		private Long payAmount;

		public Long getPayAmount() {
			return payAmount;
		}

		public void setPayAmount(Long payAmount) {
			this.payAmount = payAmount;
		}

		public Integer getUserId() {
			return userId;
		}

		public void setUserId(Integer userId) {
			this.userId = userId;
		}
	}

The above example is a traditional form of invocation. The business code decoupling form is as follows. For more specific usage, please refer to demo(interfacecall):

	@Transactional
	public String buySomething(int userId,long money){
		
		int id = saveOrderRecord(jdbcTemplate,userId,money);
		
		//WalletPayRequestVOjust need to implements Serializable
		WalletPayRequestVO request = new WalletPayRequestVO();
		request.setUserId(userId);
		request.setPayAmount(money);
		
		//payService is an framework generated object of a user customed interface without any super classes
		WalletPayResponseVO pay = payService.pay(request);

		return "id:" + id + " freeze:" + pay.getFreezeAmount();
		}

#### more examples
please refer in the directory easytrans-demo
more completed configuration and usage please refer in UT of easytrans-starter

### configuration

Each business database needs to add two tables.

	-- to record whether the global-transaction status
	-- the fileds start with 'p_'  represents the parent transaction ID corresponding to this transaction.
	-- When select for update executed, if transaction ID corresponding record does not exist, transaction must be failed.
	-- Records exist, but status 0 indicates transaction success, and 1 indicates transaction failure (including parent transaction and this transaction)
	- the record exists, but status is 2 indicating that the final state of the transaction is unknown.
	CREATE TABLE `executed_trans` (
	  `app_id` smallint(5) unsigned NOT NULL,
	  `bus_code` smallint(5) unsigned NOT NULL,
	  `trx_id` bigint(20) unsigned NOT NULL,
	  `p_app_id` smallint(5) unsigned DEFAULT NULL,
	  `p_bus_code` smallint(5) unsigned DEFAULT NULL,
	  `p_trx_id` bigint(20) unsigned DEFAULT NULL,
	  `status` tinyint(1) NOT NULL,
	  PRIMARY KEY (`app_id`,`bus_code`,`trx_id`),
	  KEY `parent` (`p_app_id`,`p_bus_code`,`p_trx_id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
	
	CREATE TABLE `idempotent` (
	  `src_app_id` smallint(5) unsigned NOT NULL COMMENT 'source AppID',
	  `src_bus_code` smallint(5) unsigned NOT NULL COMMENT 'source business code',
	  `src_trx_id` bigint(20) unsigned NOT NULL COMMENT 'source transaction ID',
	  `app_id` smallint(5) NOT NULL COMMENT 'invoked APPID',
	  `bus_code` smallint(5) NOT NULL COMMENT 'invoked business code',
	  `call_seq` smallint(5) NOT NULL COMMENT 'invokded sequence of the same businesss code within a global-transaction',
	  `handler` smallint(5) NOT NULL COMMENT 'handler appid',
	  `called_methods` varchar(64) NOT NULL COMMENT 'invoked methods',
	  `md5` binary(16) NOT NULL COMMENT 'request parameter MD5',
	  `sync_method_result` blob COMMENT 'business called result',
	  `create_time` datetime NOT NULL COMMENT 'executed time',
	  `update_time` datetime NOT NULL,
	  `lock_version` smallint(32) NOT NULL COMMENT 'lock version',
	  PRIMARY KEY (`src_app_id`,`src_bus_code`,`src_trx_id`,`app_id`,`bus_code`,`call_seq`,`handler`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;


（RDBS based distributed transaction log，if you use REDIS,then it's unnecessary）You need to have a RDBS that record transaction logs and create two tables for it. Each business service must have a transaction log database. Multiple services can share one transaction log database.

	CREATE TABLE `trans_log_detail` (
	  `log_detail_id` int(11) NOT NULL AUTO_INCREMENT,
	  `trans_log_id` binary(12) NOT NULL,
	  `log_detail` blob,
	  `create_time` datetime NOT NULL,
	  PRIMARY KEY (`log_detail_id`),
	  KEY `app_id` (`trans_log_id`)
	) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
	
	CREATE TABLE `trans_log_unfinished` (
	  `trans_log_id` binary(12) NOT NULL,
	  `create_time` datetime NOT NULL,
	  PRIMARY KEY (`trans_log_id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
	SELECT * FROM translog.trans_log_detail;

## extension

The framework use interface to glue each module, it is flexible expansion. The following modules are recommended to extend:

* RPC implementation
	* support for Alibaba DUBBO, SPRING CLOUD RIBBON/EUREKA at present.
	* welcome additional implementation.
* message queue implementation
	* support for Alibaba ONS at present (using unordered messages when creating) and KAFKA
	* welcomes additional implementation and requires two core approaches.
* serialization implementation
	* The serialization provided by Spring-core
	* welcomes additional implementation to enhance efficiency.
* Filter implementation
	* there are idempotent Filter, metadata setting FILTER, and nested transaction assistance handling Filter.
	* new Filter can be added if additional requirements are available.
* implementation of data source selector
	* currently provides single data source selector.
	* If the service has multiple data sources, it needs to implement a business-related data source selector and select the data source according to the request
* implementation of transaction execution logs
	* the implementation of relational database and REDIS is currently available.
	* To improve efficiency, you can implement transaction logs based on other forms, such as file systems, HBASE, etc. Welcome PR
* master slave selector
	* implementation of master-slave selection based on ZK.
	* if you don't want to use ZK, you can replace ZK yourself.

## best practices

transaction logging based on Database

*  use different server for the transaction log database and the business database.


The parameters and return values

* because of the persistence cost, ensure that the parameters and return values of the calling method as small as possible.




## FAQ

1. how to determine whether a global-transaction is commited after CRASH?

* when invoking the startEasyTrans () method, the framework inserts a record into executed_trans.
* the remote transaction method can only be executed after the startEasyTrans () method is called.
* The business initiator (master transaction) holds the lock of the executed_trans record until the master transaction rolls back or commits
* So when the CRASH recovery process uses select for update to query the executed_trans record, it is bound to get an accurate result of whether or not it has been submitted (if the master transaction is still in progress, select for update will wait)
* Use select for update to avoid misquerying the global-transaction status result in MVCC

## Others
wechat public account of author
![wechat public account](https://raw.githubusercontent.com/QNJR-GROUP/ImageHub/master/easytrans/wechat_public_account.jpg)
if you like this framework，please STAR it,THX



# 中文
## 零、SEO
柔性事务，分布式事务，TCC，SAGA，可靠消息，最大努力交付消息，事务消息，补偿，全局事务，soft transaction, distribute transaction, compensation

本框架可一站式解决分布式SOA（包括微服务等）的事务问题。

## 一、由来 及 特性
这个框架是结合公司之前遇到的分布式事务场景以及 支付宝程立分享的一个PPT<大规模SOA系统的分布式事务处理>而设计实现。

本框架意在解决之前公司对于每个分布式事务场景中都自行重复设计 中间状态、幂等实现及重试逻辑 的状况。

采纳本框架后能解决现有已发现的所有分布式事务场景，减少设计开发设计工作量，提高开发效率，并统一保证事务实现的可靠性。

特性：

* 一个框架包含多种事务形态，一个框架搞定所有类型的事务
* 多种事务形态可混合使用
* 高性能,大多数业务系统瓶颈在业务数据库，若不启用框架的幂等功能，对业务数据库的额外消耗仅为写入25字节的一行
* 可选的框架自带幂等实现及调用错乱次序处理，大幅减轻业务开发工作量,但启用的同时会在业务数据库增加一条幂等控制行
* 业务代码可实现完全无入侵
* 支持递归事务
* 无需额外部署协调者，不同APP的服务协调自身发起的事务，也避免了单点故障
	* 也可以对某个APP单独部署一个协调者
* 分布式事务ID可关联业务ID，业务类型，APPID，便于监控各个业务的分布式事务执行情况


## 二、分布式事务场景及框架对应实现

### 分布式事务场景
* 无需分布式事务
    * 最常用
    * 最优先使用
* 使用消息队列完成的最终一致性事务
    * 适用于服务可以自行决定全局事务状态（rollback/commit）的场景
    * 常见
    * 若一定要与其他服务写接口发生交互，则优先使用
    * 依据是否保证投递到订阅者，分为可靠消息及最大努力交付消息
    * 有时业务要求一些本质是异步的操作同步返回结果，若同步返回失败则后台异步补单。这种业务本质也归属于无需外部数据变更以协助完成的最终一致性，但介于其同步时要返回结果，其有区别于可靠消息。
* 使用传统补偿完成的最终一致性事务
    * 适用于需要获取远程执行结果来决定逻辑事务走向 且 可以进行补偿的业务
    * 次常见
    * 若使用消息队列不能解决的事务问题优先考虑使用基于补偿的最终一致性事务
* 使用TCC完成最终一致性事务
    * 适用于需要获取远程执行结果来决定逻辑事务走向 且 不可以进行补偿的业务
    * 最不常见
    * 最终解决办法，囊括所有必须使用2PC实现的场景。编码量最大，性能消耗最大，应尽量避免使用本类型的事务
* SAGA
	* 其与传统补偿相似
	* 但区别为正向操作和反向操作都不在主控事务执行过程中执行，通过队列异步执行，因此可以减少事务记录加锁时间
	* 若同步操作较多（TCC、补偿）的情况下，可以用SAGA替代同步操作，这样会有性能上的提升
	* 但使用SAGA其主控方代码形态会有稍有变化，因为其主控方的事务要拆成两个(具体请查看DEMO),有额外编码负担
	* 本框架实现的SAGA并非传统的SAGA,可以将其类比为异步TCC，与传统SAGA的区别可以类比 传统补偿及TCC的关系
	* 还有就是本框架使用RPC而非队列实现SAGA，具体原因可以在SAGA-TCC这个DEMO里查看

### 框架对应实现及基本原理
框架实现了上述所有事务场景的解决方案，并提供了统一易用的接口。以下介绍基本实现原理


#### 无需分布式事务
对于此类事务，框架完全不介入，不执行一行额外代码

#### 其他事务场景
框架的核心依赖是Spring的TransactionSynchronization，只要使用的事务管理器继承自AbstractPlatformTransactionManager都能使用本框架（基本上事务管理器都继承自本实现）,在此之外，1.0.0版本之后，框架使用了SPRING BOOT的配置功能及JDK8的特性，因此SPRING BOOT及JDK8也是必选项

对于分布式事务，框架会在调用远程事务方法前，将对应的框架操作挂载到TransactionSynchronization中，如：
* 使用消息队列完成的最终一致性事务，框架将会在全局事务状态确认为提交后发发送消息，保证只有全局事务确认后才能被外部看见，这里也省去业务开发者对于 发送-确认-检测 类型 队列实现的代码量
* 使用TCC完成最终一致性事务,框架将会根据事务的实际完成情况调用Confirm或者Cancel,用传统补偿完成的最终一致性事务也类似

框架有后台线程负责CRASH恢复，其根据“在执行分布式服务调用前写入的WriteAheadLog获得可能已经调用的业务”以及“跟随业务一起提交的一条框架记录以确认的业务最终提交状态”来进行最终的CRASH具体操作（如TCC的Confirm或者Rollback）

框架对于幂等也有完整的实现（可选），框架能保证业务方法逻辑上只执行一遍（有可能执行多遍，但多次执行的方法会被回滚掉，因此，涉及不可回滚的外部资源时，业务程序需自行把控其幂等性）

框架对于方法间有调用关系依赖的也进行妥善的处理，例如基于传统补偿完成的最终一致性事务中
* 业务方法没有被调用(或者由于延时晚于补偿方法执行)，那么补偿方法对应的业务实现也不会被调用（但框架仍会记录下补偿方法已经被调用过）
* 补偿方法已经被掉用过了，那么业务方法无论曾经有没有被执行过，都不会被调用

所有远程调用的结果都是以Future形式返回，这给框架的性能优化提供了空间，在第一次获取结果前，所有的日志都不会被写入所有远程方法都不会被调用。一旦业务程序尝试获取执行结果时，才会批量写入日志及后续并发调用远程方法。

如果业务程序没有尝试获取执行结果，框架COMMIT前会统一尝试GET一遍，对于所有远程方法一旦抛出了Exception,框架都会在最后commit前将业务回滚，而无论之前是否catch住了，这样能保证编程模型的简洁，方便写出正确易理解的代码。

## 三、使用简介

### 业务代码

#### 业务发起者
对于业务发起者，框架只暴露一个接口

    public interface EasyTransFacade {
	/**
	 * start easy Transaction
	 * @param busCode appId+busCode+trxId is a global unique id,no matter this transaction commit or roll back
	 * @param trxId see busCode
	 */
	void startEasyTrans(String busCode, String trxId);

	/**
	 * execute remote transaction
	 * @param params
	 * @return
	 */
	<P extends EasyTransRequest<R, E>, E extends EasyTransExecutor, R extends Serializable> Future<R> execute(P params);
    }

使用方法如下，使用方直接调用远程方法即可，无需考虑具体的分布式事务类型及后续处理：

    @Transactional
    public void buySomething(int userId,long money){
    /**
		* 本地业务方法，下订单，并获得订单号
		*/
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate();
		Integer id = saveOrderRecord(jdbcTemplate,userId,money);
		
		/**
		 * 声明全局事务ID，其由appId,业务代码，业务代码内ID构成
		 * 这个方法也可以不调用，框架用默认的业务代码及自动生成的ID号，但这样做的缺点是全局事务ID无法直接关联到具体业务
		 * 不过这个可以通过自定义BusinessCodeGenerator及TrxIdGenerator来产生关联
		 */
		transaction.startEasyTrans(BUSINESS_CODE, String.valueOf(id));
		
		/**
		 * 调用远程服务扣除所需的钱,这个远程服务实现了TCC接口,
		 * 框架会根据buySomething方法的事务结果来维护远程服务的最终一致性
		 */
		WalletPayTccMethodRequest deductRequest = new WalletPayTccMethodRequest();
		deductRequest.setUserId(userId);
		deductRequest.setPayAmount(money/2);
		// 业务上可多次调用同一方法，最后生效的也是实际调用的次数
		Future<WalletPayTccMethodResult> deductFuture = transaction.execute(deductRequest);
		Future<WalletPayTccMethodResult> deductFuture = transaction.execute(deductRequest);

		/**
		 * 调用远程服务进行账务登记，账务服务是一个可补偿服务
		 * 当buySomething方法对应的事务回滚了，框架将会自动调用补偿对应的业务方法
		 * 
		 */
		AccountingRequest accountingRequest = new AccountingRequest();
		accountingRequest.setAmount(money);
		accountingRequest.setUserId(userId);
		Future<AccountingResponse> accountingFuture = transaction.execute(accountingRequest);
		
	     /**
		 * 
		 * 发布消息以触发相关的业务处理，例如增加积分。这是一个可靠的消息。
		 * 这个消息会在buySomething()的事务提交后，保证成功发布出去
		 * 但至于消息是否成功消费，这取决于Queue接口的具体实现
		 */
		OrderMessage orderMessage = new OrderMessage();
		orderMessage.setUserId(userId);
		orderMessage.setAmount(money);
		Future<PublishResult> reliableMessage = transaction.execute(orderMessage);
    }



#### 服务提供者
对于服务提供者，则实现对应的接口，并将其注册到Spring的Bean容器即可

如提供TCC服务的业务，需实现TccMethod接口：

     public interface TccMethod<P extends TccTransRequest<R>, R  extends Serializable> extends RpcBusinessProvider<P> {
    	R doTry(P param);
    	void doConfirm(P param);
    	void doCancel(P param);
    }

具体实现：
   
    @Component
    public class WalletPayTccMethod implements TccMethod<WalletPayTccMethodRequest, WalletPayTccMethodResult>{

	@Resource
	private WalletService wlletService;

	@Override
	public WalletPayTccMethodResult doTry(WalletPayTccMethodRequest param) {
		return wlletService.doTryPay(param);
	}

	@Override
	public void doConfirm(WalletPayTccMethodRequest param) {
		wlletService.doConfirmPay(param);
	}

	@Override
	public void doCancel(WalletPayTccMethodRequest param) {
		wlletService.doCancelPay(param);
	}
    }

其中WalletPayTccMethodRequest是请求参数，其为继承自EasyTransRequest类的POJO,且其需要添加BusinessIdentifer注解，以便于框架确认本请求对应的业务ID

	@BusinessIdentifer(appId=Constant.APPID,busCode=METHOD_NAME)
	public class WalletPayTccMethodRequest implements TccTransRequest<WalletPayTccMethodResult>{
		private static final long serialVersionUID = 1L;
		
		private Integer userId;
		
		private Long payAmount;

		public Long getPayAmount() {
			return payAmount;
		}

		public void setPayAmount(Long payAmount) {
			this.payAmount = payAmount;
		}

		public Integer getUserId() {
			return userId;
		}

		public void setUserId(Integer userId) {
			this.userId = userId;
		}
	}

以上的示例是传统的调用形式，无业务代码入侵的使用形式如下,更具体的使用请参考demo(interfacecall)：


	@Transactional
	public String buySomething(int userId,long money){
		
		int id = saveOrderRecord(jdbcTemplate,userId,money);
		
		//WalletPayRequestVO 是一个VO，无任何继承及注解，只有相关的属性及GETTER,SETTER
		WalletPayRequestVO request = new WalletPayRequestVO();
		request.setUserId(userId);
		request.setPayAmount(money);
		
		//payService是通过框架生成的一个PayService接口实例，调用该实例的方法即完成了分布式事务调用
		WalletPayResponseVO pay = payService.pay(request);
		

		return "id:" + id + " freeze:" + pay.getFreezeAmount();
		}

#### 更多例子
请参考easytrans-demos里面的代码，这里提供了一个简明的代码。
更完整的配置及相关用例请参考easytrans-starter里的UT案例，UT中有一个MockSerivice,使用了各种场景的事务。并对事务的各种异常场景做了测试。

### 运行配置

每个运行业务的库都需要新增两张表,表的字段类型经过了修改，若之前建了表，需要重建

    -- 用于记录业务发起方的最终业务有没有执行
    -- p_开头的，代表本事务对应的父事务id
    -- select for update查询时，若事务ID对应的记录不存在则事务一定失败了
    -- 记录存在，但status为0表示事务成功,为1表示事务失败（包含父事务和本事务）
    -- 记录存在，但status为2表示事务最终状态未知
	CREATE TABLE `executed_trans` (
	  `app_id` smallint(5) unsigned NOT NULL,
	  `bus_code` smallint(5) unsigned NOT NULL,
	  `trx_id` bigint(20) unsigned NOT NULL,
	  `p_app_id` smallint(5) unsigned DEFAULT NULL,
	  `p_bus_code` smallint(5) unsigned DEFAULT NULL,
	  `p_trx_id` bigint(20) unsigned DEFAULT NULL,
	  `status` tinyint(1) NOT NULL,
	  PRIMARY KEY (`app_id`,`bus_code`,`trx_id`),
	  KEY `parent` (`p_app_id`,`p_bus_code`,`p_trx_id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
	
	CREATE TABLE `idempotent` (
	  `src_app_id` smallint(5) unsigned NOT NULL COMMENT '来源AppID',
	  `src_bus_code` smallint(5) unsigned NOT NULL COMMENT '来源业务类型',
	  `src_trx_id` bigint(20) unsigned NOT NULL COMMENT '来源交易ID',
	  `app_id` smallint(5) NOT NULL COMMENT '调用APPID',
	  `bus_code` smallint(5) NOT NULL COMMENT '调用的业务代码',
	  `call_seq` smallint(5) NOT NULL COMMENT '同一事务同一方法内调用的次数',
	  `handler` smallint(5) NOT NULL COMMENT '处理者appid',
	  `called_methods` varchar(64) NOT NULL COMMENT '被调用过的方法名',
	  `md5` binary(16) NOT NULL COMMENT '参数摘要',
	  `sync_method_result` blob COMMENT '同步方法的返回结果',
	  `create_time` datetime NOT NULL COMMENT '执行时间',
	  `update_time` datetime NOT NULL,
	  `lock_version` smallint(32) NOT NULL COMMENT '乐观锁版本号',
	  PRIMARY KEY (`src_app_id`,`src_bus_code`,`src_trx_id`,`app_id`,`bus_code`,`call_seq`,`handler`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;


（基于数据库实现的事物日志，若使用REDIS记录事务日志则无需以下表）需要有一个记录事务日志的数据库，并为其创建两张表。每个业务服务都必须有对应的事务日志数据库。可多个服务共用一个，也可以一个服务单独一个事务日志。

	CREATE TABLE `trans_log_detail` (
	  `log_detail_id` int(11) NOT NULL AUTO_INCREMENT,
	  `trans_log_id` binary(12) NOT NULL,
	  `log_detail` blob,
	  `create_time` datetime NOT NULL,
	  PRIMARY KEY (`log_detail_id`),
	  KEY `app_id` (`trans_log_id`)
	) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
	
	CREATE TABLE `trans_log_unfinished` (
	  `trans_log_id` binary(12) NOT NULL,
	  `create_time` datetime NOT NULL,
	  PRIMARY KEY (`trans_log_id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
	SELECT * FROM translog.trans_log_detail;

详细的配置会后续作出手册，但在此之前各位可以参考easytrans-starter里的UT案例 或者 参考demo进行


## 四、扩展性
框架采用接口粘合各个模块，具有较强的扩展性，推荐 扩展、替换 的模块：
* RPC实现
    * 目前支持DUBBO，SPRING CLOUD RIBBON/EUREKA的支持
    * 欢迎增加额外实现
* 消息队列实现
    * 目前支持阿里ONS(创建时请使用无序消息)及KAFKA
    * 欢迎增加额外实现，需实现两个核心方法
* 序列化实现
    * 目前使用Spring-core提供的序列化，效率较低，但目前性能不是瓶颈，没做优化
    * 欢迎增加额外实现以提高效率
* 增删改Filter具体实现类
    * 目前有幂等Filter、元数据设置FILTER、级联事务协助处理Filter
    * 若有额外需求可新增Filter
* 数据源选择器的实现
    * 目前提供单数据源选择器
    * 若服务有多个数据源，需自行实现业务相关的数据源选择器，根据请求选择数据源
* 事务执行日志的实现
    * 目前提供基于关系数据库及REDIS的实现
    * 为提高效率，可自行实现基于其他形式的事务日志，如文件系统，HBASE等，欢迎PR
* 主从选择器
    * 目前基于ZK实现了主从选择
    * 若不想采用ZK可自行替换ZK实现

## 五、最佳实践

### 基于数据库的事务日志
* 将事务日志数据库与业务数据库分开存放

### 参数及返回值
* 因有持久化成本，请保证调用方法的参数及返回值尽量小


## 六、FAQ
1. 如何在CRASH后判断一个柔性事务是否提交？
	* 在调用startEasyTrans()方法时，框架将插入一条记录到executed_trans中
	* 在调用startEasyTrans()方法后，才可以执行远程事务方法
	* 业务发起者（主控事务）将持有executed_trans记录的锁直到主控事务回滚或者提交
	* 因此CRASH恢复进程使用select for update 查询executed_trans记录时，必然能得到准确的是否已经提交的结果（若主控事务仍在进行中，select for update将会等待）
	* 使用select for update是为了避免在MVCC情况下错误查询出最终事务提交结果的情况

## 七、其他
欢迎加作者个人微信公众号

![wechat public account](https://raw.githubusercontent.com/QNJR-GROUP/ImageHub/master/easytrans/wechat_public_account.jpg)

若觉得框架不错，希望能STAR,THX








