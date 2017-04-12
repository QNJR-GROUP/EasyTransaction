package com.yiqiniu.easytrans.rpc;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class EasyTransRpcProviderInitializer {
	
	@Resource
	private EasyTransFilterChainFactory filterFactory;
	
	@Resource
	private EasyTransRpcProvider rpcProvider;
	
	@Resource
	private ListableProviderFactory wareHouse;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init(){
		
		//set filters
		rpcProvider.addEasyTransFilter(filterFactory.getDefaultFilters());;
		
		Set<Class<?>> typeSet = wareHouse.getServiceTransactionTypeSet(RpcBusinessProvider.class);
		
		//start services
		for(Class<?> transcationType:typeSet){
			List<Object> services = wareHouse.getServices(RpcBusinessProvider.class, transcationType);
			
			HashMap<BusinessIdentifer,RpcBusinessProvider<?>> map = new HashMap<BusinessIdentifer, RpcBusinessProvider<?>>(services.size());
			for(Object serviceObject:services){
				RpcBusinessProvider<?> provider = (RpcBusinessProvider<?>) serviceObject;
				Class<? extends EasyTransRequest<?, ?>> requestClass = ReflectUtil.getRequestClass((Class<? extends BusinessProvider<?>>) provider.getClass());
				BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(requestClass);
				map.put(businessIdentifer, provider);
			}
			
			rpcProvider.startService(transcationType, map);
		}
	}
	
}
