package com.yiqiniu.easytrans.queue.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.MessageBusinessProvider;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;
import com.yiqiniu.easytrans.util.ReflectUtil;


/**
 * register the message handlers,wire the filters,dispatch handlers  
 */
public class EasyTransMsgInitializer implements EasyTransMsgListener {

	private ConcurrentHashMap<Class<? extends EasyTransRequest<?, ?>>, EasyTransFilter> mapHandler = new ConcurrentHashMap<Class<? extends EasyTransRequest<?, ?>>, EasyTransFilter>();
	
	private ListableProviderFactory serviceWareHouse;
	private EasyTransMsgConsumer consumer;
	private EasyTransFilterChainFactory filterChainFactory;
	private String applicationName;
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private EasyTransFilter consumeStatusCheckFilter = new EasyTransFilter(){

		@Override
		public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String, Object> header,
				EasyTransRequest<?, ?> request) {
			EasyTransResult result = filterChain.invokeFilterChain(header, request);
			if(result.getValue() == null && result.getException() == null 
					|| result.getValue().equals(EasyTransConsumeAction.ReconsumeLater)){
				result.setException(new NeedToReconsumeLaterException());//help to roll back in idempotent filter
			}
			return result;
		}
		
	};
	
	
	
	public EasyTransMsgInitializer(ListableProviderFactory serviceWareHouse, EasyTransMsgConsumer consumer,
			EasyTransFilterChainFactory filterChainFactory, String applicationName) {
		super();
		this.serviceWareHouse = serviceWareHouse;
		this.consumer = consumer;
		this.filterChainFactory = filterChainFactory;
		this.applicationName = applicationName;
		init();
	}
	
	@SuppressWarnings({ "unchecked" })
	private void init(){
		
		HashMap<String,List<String>> map = new HashMap<String, List<String>>();

		//do initial
		Set<Class<?>> transactionTypeSet = serviceWareHouse.getServiceTransactionTypeSet(MessageBusinessProvider.class);
		for(Class<?> transactionType:transactionTypeSet){
			List<Object> serviceMap = serviceWareHouse.getServices(MessageBusinessProvider.class, transactionType);
			for(Object handler:serviceMap){
				BusinessProvider<?> messageHandler = (BusinessProvider<?>) handler;
				wrapToFilter(messageHandler);
				Class<? extends EasyTransRequest<?, ?>> clazz = ReflectUtil.getRequestClass((Class<? extends BusinessProvider<?>>) messageHandler.getClass());
				BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(clazz);
				
				List<String> list = map.get(businessIdentifer.appId());
				if(list == null){
					list = new ArrayList<String>();
					map.put(businessIdentifer.appId(), list);
				}
				list.add(businessIdentifer.busCode());
			}
		}
		
		//register listener to queue
		for(Entry<String, List<String>> e:map.entrySet()){
			consumer.subscribe(e.getKey(), e.getValue(), this);
		}
		consumer.start();
	}
	
	@Override
	public EasyTransConsumeAction consume(Map<String,Object> header, EasyTransRequest<?, ?> message) {
		
		EasyTransFilter easyTransFilter = getFilter(message);
		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(message.getClass());
		EasyTransFilterChain filterChain = filterChainFactory.getDefaultFilterChain(applicationName/*should use consumer's appId*/, businessIdentifer.busCode(), EasyTransFilterChain.MESSAGE_BUSINESS_FLAG);
		filterChain.addFilter(consumeStatusCheckFilter);
		filterChain.addFilter(easyTransFilter);
		EasyTransResult result = filterChain.invokeFilterChain(header,message);
		
		EasyTransConsumeAction consumeResult = (EasyTransConsumeAction) result.getValue();
		if(consumeResult == null){
			result.setValue(EasyTransConsumeAction.ReconsumeLater);
		}
		
//		if(result.getException() != null && result.getException().getClass() != NeedToReconsumeLaterException.class){
//			LOG.error("Consume Error!",result.getException());
//		}
		
		return consumeResult;
	}
	



	private EasyTransFilter getFilter(EasyTransRequest<?, ?> message) {
		return mapHandler.get(message.getClass());
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void wrapToFilter(final BusinessProvider<?> handler) {
		
		List<Class<?>> result = ReflectUtil.getTypeArguments(MessageBusinessProvider.class, (Class)handler.getClass());
		if(result != null){
			Class<? extends EasyTransRequest<?, ?>> requestClass = (Class<? extends EasyTransRequest<?, ?>>) result.get(0);
			
			EasyTransFilter easyTransFilter = new EasyTransFilter() {
				
				@Override
				public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String,Object> header,
						EasyTransRequest<?, ?> request) {
					EasyTransResult easyTransResult = new EasyTransResult();
					
					try {
						easyTransResult.setValue(((MessageBusinessProvider<?>)handler).consume(request));
						if(easyTransResult.getValue() == EasyTransConsumeAction.CommitMessage) {
							logger.info("EasyTrans message consume Success:" + request);
						} else {
							logger.warn("EasyTrans message consume later:" + request);
						}
					} catch (Throwable e) {
						easyTransResult.setException(e);
						logger.error("EasyTrans message consume Exception Occour:" + request,e);
					}
					return easyTransResult;
				}
			};
			mapHandler.put(requestClass, easyTransFilter);
		}
	}
	
	public static class NeedToReconsumeLaterException extends RuntimeException{
		private static final long serialVersionUID = 1L;
	}

}