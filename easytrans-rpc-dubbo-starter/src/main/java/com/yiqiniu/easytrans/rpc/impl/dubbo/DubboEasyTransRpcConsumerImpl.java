package com.yiqiniu.easytrans.rpc.impl.dubbo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.yiqiniu.easytrans.executor.RelativeInterface;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class DubboEasyTransRpcConsumerImpl implements EasyTransRpcConsumer{
	
	private String applicationName;
	private String dubboZkUrl;
	
	public DubboEasyTransRpcConsumerImpl(String applicationName, String dubboZkUrl) {
		super();
		this.applicationName = applicationName;
		this.dubboZkUrl = dubboZkUrl;
	}

	private Map<String,GenericService> mapRef = new ConcurrentHashMap<String,GenericService>();
	
	@SuppressWarnings("unchecked")
	@Override
	public <P extends EasyTransRequest<R, ?>, R extends Serializable> R call(String appId, String busCode, String innerMethod, Map<String,Object> header, P params) {
		
		GenericService genericService = getCaller(appId, busCode,params);
		
		Object result = genericService.$invoke(innerMethod, new String[]{params.getClass().getName(), header.getClass().getName()}, new Object[]{params,header});
//		JsonElement jsonTree = gson.toJsonTree(result);
//		return (R) gson.fromJson(jsonTree, getResultClass(params));
		return (R)result;
	}

	
//	private ConcurrentHashMap<Class<?>, Class<?>> mapResultClazz = new ConcurrentHashMap<Class<?>, Class<?>>();
//	@SuppressWarnings("unchecked")
//	private <P extends EasyTransRequest<?, ?>> Class<?> getResultClass(P params) {
//		
//		Class<?> resultClazz = mapResultClazz.get(params.getClass());
//		if(resultClazz == null){
//			resultClazz = ReflectUtil.getResultClass(params.getClass());
//			mapResultClazz.put(params.getClass(), resultClazz);
//		}
//		return resultClazz;
//	}

	private GenericService getCaller(String appId, String busCode,EasyTransRequest<?,?> request) {
		GenericService genericService = mapRef.get(getTargetKey(appId,busCode));
		if(genericService == null){
			
			List<Class<?>> typeArguments = ReflectUtil.getTypeArguments(EasyTransRequest.class, request.getClass());
			Class<?> executorCLass = typeArguments.get(1);
			RelativeInterface annotation = executorCLass.getAnnotation(RelativeInterface.class);
			Class<?> value = annotation.value();
			ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<GenericService>();
			referenceConfig.setInterface(value.getName()); // 弱类型接口名 
			referenceConfig.setVersion("1.0.0"); 
			referenceConfig.setGeneric(true); // 声明为泛化接口 
			referenceConfig.setApplication(new ApplicationConfig(applicationName));
			referenceConfig.setRegistry(new RegistryConfig(dubboZkUrl));
			referenceConfig.setGroup(appId + "-" + busCode);
			referenceConfig.setCheck(false);
			referenceConfig.setSticky(true);//设置粘滞连接以优化级联事务的级联提交性能
			genericService = referenceConfig.get();
			mapRef.put(getTargetKey(appId,busCode), genericService);
		}
		return genericService;
	}

	private String getTargetKey(String appId, String busCode) {
		return appId + "|" + busCode;
	}

	@Override
	public <P extends EasyTransRequest<R, ?>, R extends Serializable> void callWithNoReturn(
			String appId, String busCode, String innerMethod, Map<String,Object> header, P params) {
		GenericService caller = getCaller(appId, busCode,params);
		caller.$invoke(innerMethod, new String[]{params.getClass().getName(), header.getClass().getName()}, new Object[]{params,header});
		return;
	}

}
