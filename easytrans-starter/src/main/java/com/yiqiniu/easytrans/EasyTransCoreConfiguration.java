package com.yiqiniu.easytrans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import com.yiqiniu.easytrans.executor.TccMethodExecutor;
import com.yiqiniu.easytrans.filter.DefaultEasyTransFilterFactory;
import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.filter.ParentTrxStatusUpdateFilter;
import com.yiqiniu.easytrans.idempotent.DefaultIdempotentHandlerFilter;
import com.yiqiniu.easytrans.idempotent.IdempotentHandlerFilter;
import com.yiqiniu.easytrans.idempotent.IdempotentHelper;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.impl.kafka.EnableLogKafkaImpl;
import com.yiqiniu.easytrans.master.impl.EnableMasterZookeeperImpl;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.MessageBusinessProvider;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.provider.factory.DefaultListableProviderFactory;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgConsumer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgInitializer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgListener;
import com.yiqiniu.easytrans.queue.impl.kafka.EnableQueueKafkaImpl;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;
import com.yiqiniu.easytrans.recovery.ConsistentGuardianDaemonConfiguration;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProvider;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProviderInitializer;
import com.yiqiniu.easytrans.rpc.impl.rest.EnableRpcRestRibbonImpl;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.serialization.impl.SpringObjectSerialization;

/** 
* @author xudeyou 
*/
@Configuration
public class EasyTransCoreConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	public ConsistentGuardian consistentGuardian(TransStatusLogger transChecker, List<LogProcessor> logProcessors,
			TransactionLogWritter writer){
		
		Map<Class<?>,LogProcessor> map = new HashMap<>(logProcessors.size());
		for(LogProcessor p:logProcessors){
			map.put(p.getClass(), p);
		}
		
		return new ConsistentGuardian(transChecker, map, writer);
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransFacade.class)
	public EasyTransFacadeImpl easyTransFacadeImpl(ApplicationContext ctx, EasyTransSynchronizer synchronizer){
		return new EasyTransFacadeImpl(ctx, synchronizer);
	}
	
	@Bean
	public EasyTransSynchronizer easyTransSynchronizer(TransactionLogWritter writer, ConsistentGuardian consistentGuardian,
			TransStatusLogger transStatusLogger){
		return new EasyTransSynchronizer(writer, consistentGuardian, transStatusLogger, applicationName);
	}
	/**
	 * 不知道为何，不在两个入参上加上lazy就无法成功启动spring,会报找不到对应的bean。于是加上了lazy标签
	 * @param consumer
	 * @param publisher
	 * @param serializer
	 * @return
	 */
	@Bean
	public RemoteServiceCaller remoteServiceCaller(@Lazy EasyTransRpcConsumer consumer, @Lazy EasyTransMsgPublisher publisher,
			ObjectSerializer serializer){
		return new RemoteServiceCaller(consumer, publisher, serializer);
	}
	
	@Bean
	public AfterTransMethodExecutor afterTransMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient){
		return new AfterTransMethodExecutor(transSynchronizer, rpcClient);
	}
	
	@Bean
	public BestEffortMessageMethodExecutor BestEffortMessageMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient){
		return new BestEffortMessageMethodExecutor(transSynchronizer, rpcClient);
	}
	
	@Bean
	public CompensableMethodExecutor compensableMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient){
		return new CompensableMethodExecutor(transSynchronizer, rpcClient);
	}
	
	@Bean
	public ReliableMessageMethodExecutor reliableMessageMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient){
		return new ReliableMessageMethodExecutor(transSynchronizer, rpcClient);
	}
	
	@Bean
	public TccMethodExecutor tccMethodExecutor(@Lazy EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient){
		return new TccMethodExecutor(transSynchronizer, rpcClient);
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMsgListener.class)
	public EasyTransMsgInitializer easyTransMsgInitializer(ListableProviderFactory serviceWareHouse, EasyTransMsgConsumer consumer,
			EasyTransFilterChainFactory filterChainFactory){
		return new EasyTransMsgInitializer(serviceWareHouse, consumer, filterChainFactory, applicationName);
	}
	
	@Bean
	public EasyTransRpcProviderInitializer easyTransRpcProviderInitializer(EasyTransFilterChainFactory filterFactory, EasyTransRpcProvider rpcProvider,
			ListableProviderFactory wareHouse){
		return new EasyTransRpcProviderInitializer(filterFactory, rpcProvider, wareHouse);
	}
	

	@Bean
	@ConditionalOnMissingBean(DataSourceSelector.class)
	public SingleDataSourceSelector singleDataSourceSelector(DataSource dataSource, PlatformTransactionManager transactionManager){
		return new SingleDataSourceSelector(dataSource, transactionManager);
	}
	
	@Bean
	public DefaultEasyTransFilterFactory defaultEasyTransFilterFactory(List<EasyTransFilter> defaultFilters){
		return new DefaultEasyTransFilterFactory(defaultFilters);
	}
	
	@Bean
	@ConditionalOnMissingBean(IdempotentHandlerFilter.class)
	public DefaultIdempotentHandlerFilter idempotentHandlerFilter(IdempotentHelper helper, ObjectSerializer serializer){
		return new DefaultIdempotentHandlerFilter(helper, serializer);
	}
	
	@Bean
	public MetaDataFilter metaDataFilter(ListableProviderFactory providerFactory){
		return new MetaDataFilter(providerFactory);
	}
	
	@Bean
	public ParentTrxStatusUpdateFilter parentTrxStatusUpdateFilter(DataSourceSelector selector, TransStatusLogger transStatusLogger, EasyTransSynchronizer easyTransSynchronizer){
		return new ParentTrxStatusUpdateFilter(selector, transStatusLogger, easyTransSynchronizer);
	}
	
	@Bean
	public IdempotentHelper idempotentHelper(DataSourceSelector selector, ListableProviderFactory providerFactory){
		return new IdempotentHelper(selector, providerFactory);
	}
	
	@Bean
	@ConditionalOnMissingBean(ListableProviderFactory.class)
	public DefaultListableProviderFactory defaultListableProviderFactory(List<RpcBusinessProvider<?>> rpcBusinessProviderList,List<MessageBusinessProvider<?>> messageBusinessProviderList){
		HashMap<Class<?>, List<? extends BusinessProvider<?>>> mapProviderTypeBeans = new HashMap<Class<?>, List<? extends BusinessProvider<?>>>(2);
		mapProviderTypeBeans.put(RpcBusinessProvider.class, rpcBusinessProviderList);
		mapProviderTypeBeans.put(MessageBusinessProvider.class, messageBusinessProviderList);
		return new DefaultListableProviderFactory(mapProviderTypeBeans);
	}
	
	@ConditionalOnClass(EnableLogKafkaImpl.class)
	@EnableLogKafkaImpl
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
	
	@Bean
	public ConsistentGuardianDaemonConfiguration consistentGuardianDaemonConfiguration() {
		return new ConsistentGuardianDaemonConfiguration();
	}
	
	@Bean
	@ConditionalOnMissingBean(ObjectSerializer.class)
	public SpringObjectSerialization SpringObjectSerialization() {
		return new SpringObjectSerialization();
	}
	
	@Bean
	@ConditionalOnMissingBean(TransStatusLogger.class)
	public DefaultTransStatusLoggerImpl DefaultTransStatusLoggerImpl(DataSourceSelector selctor) {
		return new DefaultTransStatusLoggerImpl(selctor);
	}
}
