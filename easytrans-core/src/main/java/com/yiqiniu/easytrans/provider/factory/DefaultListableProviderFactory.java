package com.yiqiniu.easytrans.provider.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.MessageBusinessProvider;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.protocol.aft.AfterMasterTransMethod;
import com.yiqiniu.easytrans.protocol.autocps.AutoCpsMethod;
import com.yiqiniu.easytrans.protocol.cps.CompensableMethod;
import com.yiqiniu.easytrans.protocol.msg.BestEffortMessageHandler;
import com.yiqiniu.easytrans.protocol.msg.ReliableMessageHandler;
import com.yiqiniu.easytrans.protocol.saga.SagaTccMethod;
import com.yiqiniu.easytrans.protocol.tcc.TccMethod;
import com.yiqiniu.easytrans.util.ReflectUtil;


public class DefaultListableProviderFactory implements ListableProviderFactory,InitializingBean {
	
	private Map<Class<?>/*RpcBusiness、MessageBusiness */,Map<Class<?>/*TransactionType: TccMethod,BestEfforMessageHandler...*/,List<Object>/*Specific Business*/>> mapBusinessProvider = new HashMap<Class<?>, Map<Class<?>,List<Object>>>();
	
	private Map<String,Object> mapBusinessObject = new HashMap<String, Object>();
	private Map<String,Class<?>> mapBusinessInterface = new HashMap<String,Class<?>>();
	private ApplicationContext ctx;
	
	@SuppressWarnings("rawtypes")
    private Map<Class<?>, List<? extends BusinessProvider>> businessProviderTypeBeanMap;
	
//	public DefaultListableProviderFactory(Map<Class<?>, List<? extends BusinessProvider<?>>> businessProviderTypeBeanMap){
//		this.businessProviderTypeBeanMap = businessProviderTypeBeanMap;
//		initDefaultTypes();
//	}
	
    public DefaultListableProviderFactory(ApplicationContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        initDefaultTypes();
    }
	
	@SuppressWarnings("rawtypes")
    private void initDefaultTypes(){
	    
        businessProviderTypeBeanMap = new HashMap<Class<?>, List<? extends BusinessProvider>>(4);
        Map<String, RpcBusinessProvider> rpcList = ctx.getBeansOfType(RpcBusinessProvider.class);
        Map<String, MessageBusinessProvider> msgList = ctx.getBeansOfType(MessageBusinessProvider.class);

        businessProviderTypeBeanMap.put(RpcBusinessProvider.class, rpcList.values().stream().collect(Collectors.toList()));
        businessProviderTypeBeanMap.put(MessageBusinessProvider.class, msgList.values().stream().collect(Collectors.toList()));
    
	    
		
		{
			HashMap<Class<?>,List<Object>> rpcBusinessMap = new HashMap<Class<?>,List<Object>>();
			mapBusinessProvider.put(RpcBusinessProvider.class, rpcBusinessMap);
			
			rpcBusinessMap.put(TccMethod.class, new ArrayList<Object>());
			rpcBusinessMap.put(SagaTccMethod.class, new ArrayList<Object>());
			rpcBusinessMap.put(CompensableMethod.class, new ArrayList<Object>());
			rpcBusinessMap.put(AfterMasterTransMethod.class, new ArrayList<Object>());
			rpcBusinessMap.put(AutoCpsMethod.class, new ArrayList<Object>());
		}
		
		{
			HashMap<Class<?>,List<Object>> messageBusinessMap = new HashMap<Class<?>,List<Object>>();
			mapBusinessProvider.put(MessageBusinessProvider.class, messageBusinessMap);
			
			messageBusinessMap.put(BestEffortMessageHandler.class, new ArrayList<Object>());
			messageBusinessMap.put(ReliableMessageHandler.class, new ArrayList<Object>());
		}
		
		//handle
		for(Entry<Class<?>, Map<Class<?>, List<Object>>> entry :mapBusinessProvider.entrySet()){
			List<? extends BusinessProvider> beansOfType = businessProviderTypeBeanMap.get(entry.getKey());
			for(Object bean :beansOfType){
				for(Entry<Class<?>, List<Object>> transactionTypeList:entry.getValue().entrySet()){
					Class<?> transactionType = transactionTypeList.getKey();
					if(transactionType.isAssignableFrom(bean.getClass())){
						transactionTypeList.getValue().add(bean);
						Class<? extends EasyTransRequest<?, ?>> requestClass =  ReflectUtil.getRequestClass((BusinessProvider<?>)bean);
						BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(requestClass);
						if(businessIdentifer == null){
							throw new RuntimeException("request class did not add Annotation BusinessIdentifer,please add it! class:" + requestClass);
						}
						mapBusinessInterface.put(getBusKey(businessIdentifer.appId(), businessIdentifer.busCode()), transactionType);
						mapBusinessObject.put(getBusKey(businessIdentifer.appId(), businessIdentifer.busCode()), bean);
					}
				}
			}
		}
	}

	private String getBusKey(String appId, String busCode){
		return appId + "|" + busCode;
	}
	

	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.register.ServiceRegister#getServiceRootKey()
	 */
	@Override
	public Set<Class<?>> getServiceRootKey(){
		return Collections.unmodifiableSet(mapBusinessProvider.keySet());
	}
	
	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.register.ServiceRegister#getServiceTransactionTypeSet(java.lang.Class)
	 */
	@Override
	public Set<Class<?>> getServiceTransactionTypeSet(Class<?> rootType){
		Map<Class<?>, List<Object>> map = mapBusinessProvider.get(rootType);
		if(map != null){
			return  Collections.unmodifiableSet(map.keySet());
		}else{
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.register.ServiceRegister#getServiceMap(java.lang.Class, java.lang.Class)
	 */
	@Override
	public List<Object> getServices(Class<?> root,Class<?> transactionType){
		Map<Class<?>, List<Object>> map = mapBusinessProvider.get(root);
		if(map != null){
			List<Object> list = map.get(transactionType);
			if(list != null){
				return Collections.unmodifiableList(list);
			}
		}
		return null;
	}


	@Override
	public Object getService(String appId, String busCode) {
		return mapBusinessObject.get(getBusKey(appId, busCode));
	}


	@Override
	public Class<?> getServiceInterface(String appId, String busCode) {
		return mapBusinessInterface.get(getBusKey(appId, busCode));
	}	
}
