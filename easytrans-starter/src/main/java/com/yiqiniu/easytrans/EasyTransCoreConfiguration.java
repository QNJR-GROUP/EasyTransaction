package com.yiqiniu.easytrans;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.PlatformTransactionManager;

import com.yiqiniu.easytrans.core.ConsistentGuardian;
import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.core.EasyTransFacadeImpl;
import com.yiqiniu.easytrans.core.EasyTransSynchronizer;
import com.yiqiniu.easytrans.core.LogProcessor;
import com.yiqiniu.easytrans.core.RemoteServiceCaller;
import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.datasource.impl.DefaultTransStatusLoggerImpl;
import com.yiqiniu.easytrans.datasource.impl.SingleDataSourceSelector;
import com.yiqiniu.easytrans.executor.AfterTransMethodExecutor;
import com.yiqiniu.easytrans.executor.BestEffortMessageMethodExecutor;
import com.yiqiniu.easytrans.executor.CompensableMethodExecutor;
import com.yiqiniu.easytrans.executor.ReliableMessageMethodExecutor;
import com.yiqiniu.easytrans.executor.SagaTccMethodExecutor;
import com.yiqiniu.easytrans.executor.TccMethodExecutor;
import com.yiqiniu.easytrans.filter.DefaultEasyTransFilterFactory;
import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.filter.ParentTrxStatusUpdateFilter;
import com.yiqiniu.easytrans.idempotent.DefaultIdempotentHandlerFilter;
import com.yiqiniu.easytrans.idempotent.DefaultIdempotentTransactionDefinition;
import com.yiqiniu.easytrans.idempotent.IdempotentHandlerFilter;
import com.yiqiniu.easytrans.idempotent.IdempotentHelper;
import com.yiqiniu.easytrans.idempotent.IdempotentTransactionDefinition;
import com.yiqiniu.easytrans.idgen.BusinessCodeGenerator;
import com.yiqiniu.easytrans.idgen.TrxIdGenerator;
import com.yiqiniu.easytrans.idgen.impl.ConstantBusinessCodeGenerator;
import com.yiqiniu.easytrans.idgen.impl.ZkBasedSnowFlakeIdGenerator;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.impl.database.EnableLogDatabaseImpl;
import com.yiqiniu.easytrans.master.impl.EnableMasterZookeeperImpl;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.MessageBusinessProvider;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.provider.factory.DefaultListableProviderFactory;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;
import com.yiqiniu.easytrans.queue.IdenticalQueueTopicMapper;
import com.yiqiniu.easytrans.queue.QueueTopicMapper;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgConsumer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgInitializer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgListener;
import com.yiqiniu.easytrans.queue.impl.kafka.EnableQueueKafkaImpl;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublishResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;
import com.yiqiniu.easytrans.recovery.ConsistentGuardianDaemonConfiguration;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProvider;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProviderInitializer;
import com.yiqiniu.easytrans.rpc.impl.rest.EnableRpcRestRibbonImpl;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.serialization.impl.SpringObjectSerialization;
import com.yiqiniu.easytrans.stringcodec.StringCodec;
import com.yiqiniu.easytrans.stringcodec.impl.EnableStringCodecZookeeperImpl;
import com.yiqiniu.easytrans.util.ByteFormIdCodec;
import com.yiqiniu.easytrans.util.CallWrapUtil;
import com.yiqiniu.easytrans.util.DeafultByteFormIdCodec;

/**
 * @author xudeyou
 */
@Configuration
@ConditionalOnBean(EasyTransactionTrrigerConfiguration.class)
public class EasyTransCoreConfiguration {

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${easytrans.common.leastLogModel:true}")
	private boolean leastLogModel;
	
	@Value("${easytrans.common.tablePrefix:}")
	private String tablePrefix;
	
	@Value("${easytrans.idgen.trxId.zkSnow.zooKeeperUrl:}")
	private String zkBasedIdGenUrl;
	
	@Bean
	public ConsistentGuardian consistentGuardian(TransStatusLogger transChecker, List<LogProcessor> logProcessors,
			TransactionLogWritter writer) {

		Map<Class<?>, LogProcessor> map = new HashMap<>(logProcessors.size());
		for (LogProcessor p : logProcessors) {
			map.put(p.getClass(), p);
		}

		return new ConsistentGuardian(transChecker, map, writer, leastLogModel);
	}

	@Bean
	@ConditionalOnMissingBean(EasyTransFacade.class)
	public EasyTransFacadeImpl easyTransFacadeImpl(ApplicationContext ctx, EasyTransSynchronizer synchronizer, BusinessCodeGenerator busCodeGen, TrxIdGenerator idGen) {
		return new EasyTransFacadeImpl(ctx, synchronizer, busCodeGen, idGen);
	}
	
	@Bean
	@ConditionalOnMissingBean(BusinessCodeGenerator.class)
	public BusinessCodeGenerator businessCodeGenerator() {
		return new ConstantBusinessCodeGenerator();
	}
	
	@Bean
	@ConditionalOnMissingBean(TrxIdGenerator.class)
	public TrxIdGenerator trxIdGenerator() {
		return new ZkBasedSnowFlakeIdGenerator(zkBasedIdGenUrl,applicationName);
	}

	@Bean
	public EasyTransSynchronizer easyTransSynchronizer(TransactionLogWritter writer,
			ConsistentGuardian consistentGuardian, TransStatusLogger transStatusLogger) {
		return new EasyTransSynchronizer(writer, consistentGuardian, transStatusLogger, applicationName);
	}

	/**
	 * 不知道为何，不在两个入参上加上lazy就无法成功启动spring,会报找不到对应的bean。于是加上了lazy标签
	 * 
	 * @param optionalConsumer
	 * @param optionalPublisher
	 * @param serializer
	 * @return
	 */
	@Bean
	public RemoteServiceCaller remoteServiceCaller(Optional<EasyTransRpcConsumer> optionalConsumer,
			@Lazy Optional<EasyTransMsgPublisher> optionalPublisher, ObjectSerializer serializer,
			QueueTopicMapper queueTopicMapper) {

		EasyTransRpcConsumer consumer = optionalConsumer.orElseGet(() -> new EasyTransRpcConsumer() {

			@Override
			public <P extends EasyTransRequest<R, ?>, R extends Serializable> R call(String appId, String busCode,
					String innerMethod, Map<String, Object> header, P params) {
				throw new RuntimeException("can not find EasyTransRpcConsumer implement but try handle rpc request");
			}

			@Override
			public <P extends EasyTransRequest<R, ?>, R extends Serializable> void callWithNoReturn(String appId,
					String busCode, String innerMethod, Map<String, Object> header, P params) {
				throw new RuntimeException("can not find EasyTransRpcConsumer implement but try handle rpc request");
			}
		});

		EasyTransMsgPublisher publisher = optionalPublisher.orElseGet(() -> new EasyTransMsgPublisher() {

			@Override
			public EasyTransMsgPublishResult publish(String topic, String tag, String key, Map<String, Object> header,
					byte[] msgByte) {
				throw new RuntimeException("can not find EasyTransMsgPublisher implement but try to publish a message");
			}
		});

		return new RemoteServiceCaller(consumer, publisher, serializer, queueTopicMapper);
	}

	@Bean
	public AfterTransMethodExecutor afterTransMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer,
			RemoteServiceCaller rpcClient) {
		return new AfterTransMethodExecutor(transSynchronizer, rpcClient);
	}

	@Bean
	public BestEffortMessageMethodExecutor BestEffortMessageMethodExecutor(
			@Lazy EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient) {
		return new BestEffortMessageMethodExecutor(transSynchronizer, rpcClient);
	}

	@Bean
	public CompensableMethodExecutor compensableMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer,
			RemoteServiceCaller rpcClient) {
		return new CompensableMethodExecutor(transSynchronizer, rpcClient);
	}

	@Bean
	public ReliableMessageMethodExecutor reliableMessageMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer,
			RemoteServiceCaller rpcClient) {
		return new ReliableMessageMethodExecutor(transSynchronizer, rpcClient);
	}

	@Bean
	public TccMethodExecutor tccMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer,
			RemoteServiceCaller rpcClient) {
		return new TccMethodExecutor(transSynchronizer, rpcClient);
	}
	
	@Bean
	public SagaTccMethodExecutor sagaTccMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer,
			RemoteServiceCaller rpcClient) {
		return new SagaTccMethodExecutor(transSynchronizer, rpcClient);
	}

	@Bean
	@ConditionalOnMissingBean(EasyTransMsgListener.class)
	public EasyTransMsgInitializer easyTransMsgInitializer(ListableProviderFactory serviceWareHouse,
			Optional<EasyTransMsgConsumer> optionalConsumer, EasyTransFilterChainFactory filterChainFactory,
			QueueTopicMapper queueTopicMapper) {

		EasyTransMsgConsumer consumer = optionalConsumer.orElseGet(() -> {
			return new EasyTransMsgConsumer() {

				@Override
				public void subscribe(String topic, Collection<String> tag, EasyTransMsgListener listener) {
					throw new RuntimeException("can not find EasyTransMsgConsumer implement but try handle msg");
				}

				@Override
				public void start() {
					//do nothing
				}

				@Override
				public String getConsumerId() {
					throw new RuntimeException("can not find EasyTransMsgConsumer implement but try to get consumerId");
				}

			};
		});

		return new EasyTransMsgInitializer(serviceWareHouse, consumer, filterChainFactory, queueTopicMapper);
	}

	@Bean
	public EasyTransRpcProviderInitializer easyTransRpcProviderInitializer(EasyTransFilterChainFactory filterFactory,
			EasyTransRpcProvider rpcProvider, ListableProviderFactory wareHouse) {
		return new EasyTransRpcProviderInitializer(filterFactory, rpcProvider, wareHouse);
	}

	@Bean
	@ConditionalOnMissingBean(DataSourceSelector.class)
	public SingleDataSourceSelector singleDataSourceSelector(DataSource dataSource,
			PlatformTransactionManager transactionManager) {
		return new SingleDataSourceSelector(dataSource, transactionManager);
	}

	@Bean
	public DefaultEasyTransFilterFactory defaultEasyTransFilterFactory(List<EasyTransFilter> defaultFilters) {
		return new DefaultEasyTransFilterFactory(defaultFilters);
	}

	@Bean
	@ConditionalOnMissingBean(IdempotentTransactionDefinition.class)
	public IdempotentTransactionDefinition idempotentTransactionDefinition() {
		return new DefaultIdempotentTransactionDefinition();
	}

	@Bean
	@ConditionalOnMissingBean(IdempotentHandlerFilter.class)
	public DefaultIdempotentHandlerFilter idempotentHandlerFilter(IdempotentHelper helper, ObjectSerializer serializer,
			IdempotentTransactionDefinition idempotentTransactionDefinition) {
		return new DefaultIdempotentHandlerFilter(applicationName, helper, serializer, idempotentTransactionDefinition);
	}

	@Bean
	public MetaDataFilter metaDataFilter(ListableProviderFactory providerFactory) {
		return new MetaDataFilter(providerFactory);
	}

	@Bean
	public ParentTrxStatusUpdateFilter parentTrxStatusUpdateFilter(DataSourceSelector selector,
			TransStatusLogger transStatusLogger, EasyTransSynchronizer easyTransSynchronizer) {
		return new ParentTrxStatusUpdateFilter(selector, transStatusLogger, easyTransSynchronizer);
	}

	@Bean
	public IdempotentHelper idempotentHelper(DataSourceSelector selector, ListableProviderFactory providerFactory, StringCodec stringCodecer) {
		return new IdempotentHelper(applicationName, selector, providerFactory, stringCodecer, tablePrefix);
	}

	@Bean
	@ConditionalOnMissingBean(ListableProviderFactory.class)
	public DefaultListableProviderFactory defaultListableProviderFactory(
			Optional<List<RpcBusinessProvider<?>>> rpcBusinessProviderList,
			Optional<List<MessageBusinessProvider<?>>> messageBusinessProviderList) {
		HashMap<Class<?>, List<? extends BusinessProvider<?>>> mapProviderTypeBeans = new HashMap<Class<?>, List<? extends BusinessProvider<?>>>(
				2);

		List<RpcBusinessProvider<?>> rpcList = rpcBusinessProviderList.orElse(Collections.emptyList());
		List<MessageBusinessProvider<?>> msgList = messageBusinessProviderList.orElse(Collections.emptyList());

		mapProviderTypeBeans.put(RpcBusinessProvider.class, rpcList);
		mapProviderTypeBeans.put(MessageBusinessProvider.class, msgList);
		return new DefaultListableProviderFactory(mapProviderTypeBeans);
	}

	@ConditionalOnClass(EnableStringCodecZookeeperImpl.class)
	@EnableStringCodecZookeeperImpl
	public static class EnableDefaultStringCodecImpl {
	}
	
	@ConditionalOnClass(EnableLogDatabaseImpl.class)
	@EnableLogDatabaseImpl
	public static class EnableDefaultLogImpl {
	}

	@ConditionalOnClass(EnableQueueKafkaImpl.class)
	@EnableQueueKafkaImpl
	public static class EnableDefaultQueueImpl {
	}

	@ConditionalOnClass(EnableRpcRestRibbonImpl.class)
	@EnableRpcRestRibbonImpl
	public static class EnableDefaultRpcImpl {
	}

	@ConditionalOnClass(EnableMasterZookeeperImpl.class)
	@EnableMasterZookeeperImpl
	public static class EnableDefaultMasterImpl {
	}

	@Import(ConsistentGuardianDaemonConfiguration.class)
	public static class EnableConsistentGuardianDaemon {
	}

	@Bean
	@ConditionalOnMissingBean(ObjectSerializer.class)
	public SpringObjectSerialization SpringObjectSerialization() {
		return new SpringObjectSerialization();
	}

	@Bean
	@ConditionalOnMissingBean(TransStatusLogger.class)
	public DefaultTransStatusLoggerImpl DefaultTransStatusLoggerImpl(DataSourceSelector selctor, StringCodec codec) {
		return new DefaultTransStatusLoggerImpl(selctor, codec, tablePrefix);
	}

	@Bean
	@ConditionalOnMissingBean(QueueTopicMapper.class)
	public QueueTopicMapper queueTopicMapper() {
		return new IdenticalQueueTopicMapper();
	}

	@Bean
	@ConditionalOnMissingBean(ByteFormIdCodec.class)
	public ByteFormIdCodec byteFormIdCodec(StringCodec codecer) {
		return new DeafultByteFormIdCodec(codecer);
	}
	
	@Bean
	public CallWrapUtil callWrappUtil(EasyTransFacade facade) {
		return new CallWrapUtil(facade);
	}
	
}
