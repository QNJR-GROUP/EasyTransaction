## 零、SEO
柔性事务，分布式事务，TCC，可靠消息，最大努力交付消息，事务消息，补偿，全局事务，soft transaction, distribute transaction, compensation

本框架可一站式解决分布式SOA（包括微服务等）的事务问题。

## 一、由来
这个框架是结合公司之前遇到的分布式事务场景以及 支付宝程立分享的一个PPT<大规模SOA系统的分布式事务处理>而设计实现。

本框架意在解决之前公司对于每个分布式事务场景中都自行重复设计 中间状态、幂等实现及重试逻辑 的状况。

采纳本框架后能解决现有已发现的所有分布式事务场景，减少设计开发设计工作量，提高开发效率，并统一保证事务实现的可靠性。

## 二、分布式事务场景及框架对应实现

### 分布式事务场景
* 无需分布式事务
    * 最常用
    * 最优先使用
* 使用消息队列完成的最终一致性事务
    * 适用于业务主逻辑无需外部数据变更协助来完成的最终一致性事务
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

### 框架对应实现及基本原理
框架实现了上述所有事务场景的解决方案，并提供了统一易用的接口。以下介绍基本实现原理

#### 无需分布式事务
对于此类事务，框架完全不介入，不执行一行额外代码

#### 其他事务场景
框架的核心依赖是Spring的TransactionSynchronization，只要使用的事务管理器继承自AbstractPlatformTransactionManager都能使用本框架（基本上事务管理器都继承自本实现）,在此之外，1.0.0版本之后，框架使用了SPRING BOOT的配置功能，因此SPRING BOOT也是必选项（除非自行配置组装各模块）。

对于分布式事务，框架会在调用远程事务方法后，将对应的框架操作挂载到TransactionSynchronization中，如：
* 使用消息队列完成的最终一致性事务，框架将会在事务COMMIT后发发送消息，保证只有COMMIT后事务才能被外部看见，这里也省去业务开发者对于 发送-确认-检测 类型 队列实现的代码量
* 使用TCC完成最终一致性事务,框架将会根据事务的实际完成情况调用Confirm或者Cancel,用传统补偿完成的最终一致性事务也类似

框架有后台线程负责CRASH恢复，其根据“在执行分布式服务调用前写入的WriteAheadLog获得可能已经调用的业务”以及“跟随业务一起提交的一条框架记录以确认的业务最终提交状态”来进行最终的CRASH具体操作（如TCC的Confirm或者Rollback）

框架对于幂等也有完整的实现（可选），框架能保证业务方法逻辑上只执行一遍（有可能执行多遍，但多次执行的方法会被回滚掉，因此，涉及不可回滚的外部资源时，业务程序需自行把控其幂等性）

框架对于方法间有调用关系依赖的也进行妥善的处理，例如基于传统补偿完成的最终一致性事务中
* 业务方法没有被调用，那么补偿方法对应的业务实现也不会被调用（但框架仍会记录下补偿方法已经被调用过）
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
		 * 如果这个方法没有被调用，那么后续的EasyTransFacade.execute方法调用会抛异常
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

#### 更多例子
请参考easytrans-starter里的UT案例，UT中有一个MockSerivice,使用了各种场景的事务。并对事务的各种异常场景做了测试。

请优先配置好环境，跑起UT，以熟悉框架的使用，需要配置的内容都在application.yml中。

### 运行配置

每个运行业务的库都需要新增两张表

    -- 用于记录业务发起方的最终业务有没有执行
    -- p_开头的，代表本事务对应的父事务id
    -- select for update查询时，若事务ID对应的记录不存在则事务一定失败了
    -- 记录存在，但status为1表示事务成功,为2表示事务失败（包含父事务和本事务）
    -- 记录存在，但status为0表示本方法存在父事务，且父事务的最终状态未知
    -- 父事务的状态将由发起方通过 优先同步告知 失败则 消息形式告知
    CREATE TABLE `executed_trans` (
      `app_id` varchar(32) CHARACTER SET utf8 NOT NULL,
      `bus_code` varchar(128) CHARACTER SET utf8 NOT NULL,
      `trx_id` varchar(64) CHARACTER SET utf8 NOT NULL,
      `p_app_id` varchar(32) CHARACTER SET utf8,
      `p_bus_code` varchar(128) CHARACTER SET utf8,
      `p_trx_id` varchar(64) CHARACTER SET utf8,
      `status` tinyint(1) NOT NULL,
      PRIMARY KEY (`app_id`,`bus_code`,`trx_id`),
      KEY `parent` (`p_app_id`,`p_bus_code`,`p_trx_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
    
    -- 记录方法调用信息，用于处理幂等
	CREATE TABLE `idempotent` (
	  `src_app_id` varchar(32) NOT NULL COMMENT '来源AppID',
	  `src_bus_code` varchar(128) NOT NULL COMMENT '来源业务类型',
	  `src_trx_id` varchar(64) NOT NULL COMMENT '来源交易ID',
	  `app_id` varchar(32) NOT NULL COMMENT '调用APPID',
	  `bus_code` varchar(128) NOT NULL COMMENT '调用的业务代码',
	  `call_seq` int(11) NOT NULL COMMENT '同一事务同一方法内调用的次数',
	  `called_methods` varchar(128) NOT NULL COMMENT '被调用过的方法名',
	  `md5` char(32) NOT NULL COMMENT '参数摘要',
	  `sync_method_result` blob COMMENT '同步方法的返回结果',
	  `create_time` datetime NOT NULL COMMENT '执行时间',
	  `update_time` datetime NOT NULL,
	  `lock_version` int(11) NOT NULL COMMENT '乐观锁版本号',
	  PRIMARY KEY (`src_app_id`,`src_bus_code`,`src_trx_id`,`app_id`,`bus_code`,`call_seq`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;


（基于数据库实现的事物日志）需要有一个记录事务日志的数据库，并为其创建两张表。每个业务服务都必须有对应的事务日志数据库。可多个服务共用一个，也可以一个服务单独一个事务日志。

    -- 记录未处理完成的事务
    CREATE TABLE `trans_log_unfinished` (
      `trans_log_id` varchar(160) NOT NULL,
      `create_time` datetime NOT NULL,
      PRIMARY KEY (`trans_log_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
    
    -- 记录详细的事务日志
    CREATE TABLE `trans_log_detail` (
      `log_detail_id` int(11) NOT NULL AUTO_INCREMENT,
      `trans_log_id` varchar(160) NOT NULL,
      `log_detail` blob,
      `create_time` datetime NOT NULL,
      PRIMARY KEY (`log_detail_id`),
      KEY `app_id` (`trans_log_id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8;

详细的配置会后续作出手册，但在此之前各位可以参考easytrans-starter里的UT案例进行


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
    * 目前提供基于数据库的执行日志读写实现,正在开发KAFKA事物执行日志的支持
    * 为提高效率，可自行实现基于其他形式的事务日志，如文件系统，HBASE等，欢迎PR
* 主从选择器
    * 目前基于ZK实现了主从选择
    * 若不想采用ZK可自行替换ZK实现

## 五、最佳实践

### 基于数据库的事务日志
* 将事务日志数据库与业务数据库分库库存放

### 参数及返回值
* 因有持久化成本，请保证调用方法的参数及返回值尽量小

## 六、FAQ
1. 如何在CRASH后判断一个柔性事务是否提交？
	* 在调用startEasyTrans()方法时，框架将插入一条记录到executed_trans中
	* 在调用startEasyTrans()方法后，才可以执行远程事务方法
	* 业务发起者（主控事务）将持有executed_trans记录的锁直到主控事务回滚或者提交
	* 因此CRASH恢复进程使用select for update 查询executed_trans记录时，必然能得到准确的是否已经提交的结果（若主控事务仍在进行中，select for update将会等待）
	* 使用select for update是为了避免在MVCC情况下错误查询出最终事务提交结果的情况
	
## 七、RELEASENOTE

目前MASTER版本为1.0.0-SNAPSHOT，升级了一个大版本，计划引入较多功能，处于不稳定的开发版本状态，其相对于之前的版本主要区别为：

* kafka消息队列实现（已完成）
* 允许同一事务多次调用同一方法（已完成）
* 解耦调用框架存储用的元数据与业务请求参数（已完成）
* 使用spring自身的配置功能代替原来自定义的配置读取功能（已完成）
* 使用spring boot风格改造代码，配置及使用更加方便（已完成）
* 事务级联功能（已完成，在事务模式里，除了传统补偿模式CompensableMethod不能进行事务级联，其他都可以进行事务级联）
* restful rpc（Spring MVC/RestTemplate/Ribbon/Eureka）实现（已完成）
* 整合2PC，完成分布式事务各种主流场景的完整解决方案（开发中）
* 同库短路设计（尚未开始）
* 独立完成的DEMO（尚未开始）
* kafka事务日志库（尚未开始）



需要注意的是

* 1.0版本并不往下兼容，如果需要升级使用，需要做简单代码调整及幂等记录表需要做简单的表结构调整
* 1.0与与1.0以下版本不能混用，需全量替换
