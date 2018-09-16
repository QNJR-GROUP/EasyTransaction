package com.yiqiniu.easytrans.rpc.impl.dubbo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProvider;

public class DubboEasyTransRpcProviderImpl implements EasyTransRpcProvider{

	private EasyTransFilterChainFactory filterChainFactory;
	private ApplicationConfig applicationConfig; 
	private RegistryConfig registryConfig;
	private ProtocolConfig protocolConfig;
	private ProviderConfig providerConfig;
	private ModuleConfig moduleConfig;
	private MonitorConfig monitorConfig;
	private DubboServiceCustomizationer customizationer;
	
	private static Logger logger = LoggerFactory.getLogger(DubboEasyTransRpcProviderImpl.class);
	
	public DubboEasyTransRpcProviderImpl(EasyTransFilterChainFactory filterChainFactory, Optional<ApplicationConfig> applicationConfig,
			Optional<RegistryConfig> registryConfig,Optional<ProtocolConfig> protocolConfig,Optional<ProviderConfig> providerConfig,Optional<ModuleConfig> moduleConfig,Optional<MonitorConfig> monitorConfig, Optional<DubboServiceCustomizationer> customizationer) {
		super();
		this.filterChainFactory = filterChainFactory;
		this.applicationConfig = applicationConfig.orElse(null);
		this.registryConfig = registryConfig.orElse(null);
		this.protocolConfig = protocolConfig.orElse(null);
		this.providerConfig = providerConfig.orElse(null);
		this.moduleConfig = moduleConfig.orElse(null);
		this.monitorConfig = monitorConfig.orElse(null);
		this.customizationer = customizationer.orElse(null);
		injectHelper = this;
	}

	private List<EasyTransFilter> filters = new ArrayList<EasyTransFilter>();
	
	private static volatile DubboEasyTransRpcProviderImpl injectHelper;
	
	public static DubboEasyTransRpcProviderImpl getInstance(){
		return injectHelper;
	}
	
	public EasyTransFilterChainFactory getFilterChainFactory() {
		return filterChainFactory;
	}
	
	

	public List<EasyTransFilter> getFilters() {
		return new ArrayList<EasyTransFilter>(filters);
	}

	@Override
	public void startService(Class<?> businessInterface,Map<BusinessIdentifer, RpcBusinessProvider<?>> businessList) {
		
		for(Entry<BusinessIdentifer, RpcBusinessProvider<?>> entry :businessList.entrySet()){
			BusinessIdentifer key = entry.getKey();
			final RpcBusinessProvider<?> value = entry.getValue();
			
			GenericService genericService = new GenericService() {
				@Override
				public Object $invoke(String method, String[] parameterTypes, Object[] args)
						throws GenericException {

					//第一个参数类型为easytransRequest,第二个为map(业务外 框架用的元数据)
					Method callMethod = getMethod(value.getClass(),method,new String[]{parameterTypes[0]});
					try {
						Object invokeResult = callMethod.invoke(value, new Object[]{args[0]});
						logger.debug("EasyTrans rpc call recived, executed success:" + args[0]);
						return invokeResult;
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						logger.info("EasyTrans rpc call recived,executed failed:" + args[0], e);
						throw new RuntimeException(e);
					}
				}
			};
			
			ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
			service.setInterface(businessInterface);
			service.setGroup(key.appId() + "-" + key.busCode());
			service.setVersion("1.0.0");
			service.setRef(genericService);
			service.setCluster("failfast");
			int rpcTimeOut = key.rpcTimeOut();
			if(rpcTimeOut != 0){
				service.setTimeout(key.rpcTimeOut());
			}
			service.setFilter("easyTransFilter");
			
			if(applicationConfig != null) {
				service.setApplication(applicationConfig);
			}
			
			if(registryConfig != null) {
				service.setRegistry(registryConfig);
			}
			
			if(protocolConfig != null) {
				service.setProtocol(protocolConfig);
			}
			
			if(monitorConfig != null) {
				service.setMonitor(monitorConfig);
			}
			
			if(moduleConfig != null) {
				service.setModule(moduleConfig);
			}
			
			if(providerConfig != null) {
				service.setProvider(providerConfig);
			}
			
			if(customizationer != null) {
				customizationer.customDubboService(key,service);
			}
			
			service.export();
		}
	}
	
	private ConcurrentHashMap<String, Method> mapMethod = new ConcurrentHashMap<String, Method>();
	private Method getMethod(Class<?> clazz,String method, String[] parameterTypes) {
		
		String methodKey = getKey(clazz,method,parameterTypes);
		Method result = mapMethod.get(methodKey);
		if (result == null) {
			try {
				Class<?>[] params = new Class<?>[parameterTypes.length];
				for (int i = 0; i < params.length; i++) {
					params[i] = Class.forName(parameterTypes[i]);
				}
				result = clazz.getMethod(method, params);
				mapMethod.put(methodKey, result);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		return result;
	}

	private String getKey(Class<?> clazz, String method, String[] parameterTypes) {
		
		StringBuilder sb = new StringBuilder(clazz.getName());
		sb.append(method);
		if(parameterTypes != null){
			for(String s:parameterTypes){
				sb.append(s);
			}
		}
		return sb.toString();
	}


	@Override
	public void addEasyTransFilter(List<EasyTransFilter> filters) {
		this.filters.addAll(filters);
	}
}
