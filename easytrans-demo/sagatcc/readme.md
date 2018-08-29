# ENGLISH
## SAGA-TCC
This demo shows how to use SAGA-TCC,the SAGA implement of this framework is not a typical one, the main difference is:
* instead of using compensation model, the framework introduce TCC, and make TCC execute  asynchronous
* instead of using queue to invoke next business, the framework use RPC
	* Mainly for the sake of mix SAGA with other transaction types, e.g. TCC、Compensation transaction、reliable message.
	* Another reason is that，if we use queue to invoke the services,it will add a lot of IOs in both queue and the transaction coordinator storage.but with RPC,we can avoid those IO
	* but the drawback is that tradition SAGA will fluent the load of downstream service, but this implement has no such effect
	
more usage you can refer to other demos or the UT case in easytrans-starter

to run this demo, you will need zookeeper and mysql,change the configuration in applicaiton.yml,you can start the services

## Attention
* SAGA-TCC transaction can only use in the master transaction

# 中文
## sagat-tcc
本demo只演示了在本框架中如何使用saga-tcc模式，本框架的saga并非传统的saga,而是融合了TCC理念的SAGA,主要区别为:
* 其加入了TCC的理念，可以在业务级别隔离保护事务中的数据，而传统SAGA只有正向操作及补偿操作
* 通知其他服务执行下一步时并没有使用队列，而是使用了RPC
	* 主要是为了兼容TCC、补偿等 同步RPC接口的混合使用，若改成消息队列通讯形态，则只能混合可靠消息等形态使用 
	* 另外一个原因是考虑到，若使用消息队列形态作为调用反馈，则每一次消息发布都需要写消息到磁盘，并且协调者也要更新自身对该事务状态的存储，IO次数较多，如果直接走RPC同步执行这些操作，则可以在内存中汇总这些执行结果，则可以省略中间这些步骤的磁盘IO，但这些IO都是业务库之外的IO，影响相对较少
	* 但带来的缺点就是，原有SAGA形态可以使用队列较好的缓冲下不同服务间的处理速度，达到削峰填谷的作用，本形态失去了这个功能


更多的更复杂的用法 请参考easytrans-demos里其他目录（demo陆续加入中） 及 easytrans-starter里面的测试案例（用法最全，最复杂）


本demo运行起来需要zk以及关系数据库，修改applicaiton.yml文件里相关zk及数据库配置后，即可启动。

## 注意点
* 框架实现的SAGA是类Orchestration-based，其只能在最顶层的主控事务里与其他事务形态混搭使用，在从事务里不能调用其他SAGA事务。
* 若实在有SAGA递归的需求，可以利用可靠消息，自行编排类Choreography-based saga事务。