package com.yiqiniu.easytrans.rpc.impl.rest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.filter.EasyTransFilterChainFactory;
import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;
import com.yiqiniu.easytrans.rpc.EasyTransRpcProvider;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ReflectUtil;

@RestController
@RequestMapping("${easytrans.rpc.rest-ribbon.provider.context:" + RestRibbonEasyTransConstants.DEFAULT_URL_CONTEXT + "}")
public class RestRibbonEasyTransRpcProviderImpl implements EasyTransRpcProvider{

	private EasyTransFilterChainFactory filterChainFactory;
	private ObjectMapper objectMapper;  
	private ObjectSerializer serializer;
	
	private Map<String,Object[]> mapCallMetaData = new HashMap<>();
	
	
	
	private static Logger logger = LoggerFactory.getLogger(RestRibbonEasyTransRpcProviderImpl.class);
	
	public RestRibbonEasyTransRpcProviderImpl(EasyTransFilterChainFactory filterChainFactory, ObjectSerializer serializer) {
		super();
		this.filterChainFactory = filterChainFactory;
		this.objectMapper = new ObjectMapper();
		this.serializer = serializer;
	}

	private List<EasyTransFilter> filters = new ArrayList<EasyTransFilter>();
	
	public EasyTransFilterChainFactory getFilterChainFactory() {
		return filterChainFactory;
	}
	
	public List<EasyTransFilter> getFilters() {
		return new ArrayList<EasyTransFilter>(filters);
	}
	
	@RequestMapping(method={RequestMethod.POST},path="/{busCode}/{innerMethod}",consumes="application/json")
	public Object easyTransMethods(@PathVariable String busCode, @PathVariable String innerMethod, @RequestHeader(RestRibbonEasyTransConstants.HttpHeaderKey.EASYTRANS_HEADER_KEY) String easyTransHeader, @RequestBody String body) throws Throwable{
		
		//通过busCode以及http method获取对应要执行的JAVA METHOD以及JAVA OBJECT
		Object[] objList = getCorrespondingObjArray(innerMethod,busCode);
		if(objList == null){
			throw new IllegalArgumentException("对应的接口尚未注册到本服务：" + busCode);
		}
		Object callObj = objList[0];
		Method callMethod = (Method) objList[1];
		@SuppressWarnings("unchecked")
		Class<? extends EasyTransRequest<?,?>> parameterClazz = (Class<? extends EasyTransRequest<?,?>>) objList[2];
		
		//将String反序列化为请求对象
		EasyTransRequest<?, ?> requestObj;
		try {
			requestObj = objectMapper.readValue(body, parameterClazz);
		} catch (IOException e) {
			throw new IllegalArgumentException("反序列化失败参数对象失败！",e);
		}
		
		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(parameterClazz);
		EasyTransFilterChain filterChain = filterChainFactory.getFilterChainByFilters(businessIdentifer.appId(), busCode, innerMethod, getFilters());
		
		filterChain.addFilter(new EasyTransFilter(){
			@Override
			public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String, Object> header,
					EasyTransRequest<?, ?> request) {
				EasyTransResult result = new EasyTransResult();
				try {
					result.setValue(callMethod.invoke(callObj, new Object[]{requestObj}));
				} catch (Exception e) {
					result.setException(e);
				}
				return result;
			}
			
		});
		
		Map<String,Object> header = deserializeHeader(easyTransHeader);
		
		EasyTransResult result;
		try {
			result = filterChain.invokeFilterChain(header,requestObj);
		} catch (Exception e) {
			logger.error("RPC EasyTrans FilterChain execute Error!",e);
			throw e;
		}
		
		
		if(result != null){
			if(result.getException() != null){
				throw result.getException();
			}
		} else {
			throw new RuntimeException("result is null!");
		}
		
		return result.getValue();
	}

	public Map<String, Object> deserializeHeader(String easyTransHeader) {
		return serializer.deserialize(Base64.getDecoder().decode(easyTransHeader));
	}
	
	private Object[] getCorrespondingObjArray(String innerMethod, String busCode) {
		return mapCallMetaData.get(getCallMetadataMapKey(innerMethod, busCode));
	}

	private String getCallMetadataMapKey(String innerMethod, String busCode) {
		return innerMethod + "|" + busCode;
	}

	@Override
	public void startService(Class<?> businessInterface,Map<BusinessIdentifer, RpcBusinessProvider<?>> businessList) {
		
		//找出businessInterface中的接口名称对应关系
		Set<Method> javaMethod2HttpMethod = new HashSet<>();
		for(Method m:businessInterface.getMethods()){
			MethodTransactionStatus annotation = m.getAnnotation(MethodTransactionStatus.class);
			if(annotation == null){
				continue;
			}
			javaMethod2HttpMethod.add(m);
		}
		
		//将这些请求映射到存储起来，需要区分HTTP方法来保存 METHOD 以及 OBJECT
		for(Entry<BusinessIdentifer, RpcBusinessProvider<?>> entry:businessList.entrySet()){
			BusinessIdentifer businessIdentifer = entry.getKey();
			RpcBusinessProvider<?> provider = entry.getValue();
			@SuppressWarnings("unchecked")
			Class<? extends EasyTransRequest<?, ?>> parameterClass = ReflectUtil.getRequestClass((Class<? extends BusinessProvider<?>>) provider.getClass());
			
			for(Method method:javaMethod2HttpMethod){
				
				//根据javaMethod2HttpMethod定义的列表，获取 JAVA METHOD
				Object[] resultArray = new Object[3];
				resultArray[0] = provider;
				resultArray[1] = method;
				resultArray[2] = parameterClass;
				mapCallMetaData.put(getCallMetadataMapKey(method.getName(), businessIdentifer.busCode()), resultArray);
			}
		}
	}


	@Override
	public void addEasyTransFilter(List<EasyTransFilter> filters) {
		this.filters.addAll(filters);
	}
}
