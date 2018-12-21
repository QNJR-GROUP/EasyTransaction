# English
(English is not my native language, can someone help me to review the text below?)

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
	* In this framework, compensation pattern do not support nested transaction, you can use TCC instead when nested transaction is required
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

###  code

Business code can introduce EasyTransaction by maven

	  <dependency>
        <groupId>com.yiqiniu.easytrans</groupId>
        <artifactId>easytrans-starter</artifactId>
        <version>1.1.3</version>
      </dependency>

This Starter contains several default implement, included: RDBS based distributed transaction log，Netflix-ribbon based http RPC implement，KAFKA based queue，if you want to replace it ,just exclude it.


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

email: skyes.xu@qq.com




