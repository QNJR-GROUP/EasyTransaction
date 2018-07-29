package com.yiqiniu.easytrans.rpc.impl.rest;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientSpecification;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.google.common.hash.Hashing;
import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.rpc.EasyTransRpcConsumer;
import com.yiqiniu.easytrans.rpc.impl.rest.RestRibbonEasyTransRpcProperties.RestConsumerProperties;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class RestRibbonEasyTransRpcConsumerImpl implements EasyTransRpcConsumer{
	
	private RestRibbonEasyTransRpcProperties properties;
	private ObjectSerializer serializer;
	List<RibbonClientSpecification> configurations;
	
	private RestTemplate loadBalancedRestTemplate;
	private LoadBalancerClient loadBalancerClient;
	
	
	public RestRibbonEasyTransRpcConsumerImpl(RestRibbonEasyTransRpcProperties properties, ObjectSerializer serializer, ApplicationContext ctx, List<RibbonClientSpecification> configurations) {
		super();
		this.properties = properties;
		this.serializer = serializer;
		this.configurations =  configurations;
		init(ctx);
	}

	private void init(ApplicationContext ctx) {
		loadBalancedRestTemplate = new RestTemplate();
		SpringClientFactory springClientFactory = springClientFactory();
		springClientFactory.setApplicationContext(ctx);
		
		loadBalancerClient = new RibbonLoadBalancerClient(springClientFactory);
		
		//custom restTemplate
		LoadBalancerRequestFactory requestFactory = new LoadBalancerRequestFactory(loadBalancerClient, Collections.emptyList());
		LoadBalancerInterceptor interceptor = new LoadBalancerInterceptor(loadBalancerClient, requestFactory);
		
		List<ClientHttpRequestInterceptor> interceptors = loadBalancedRestTemplate.getInterceptors();
		ArrayList<ClientHttpRequestInterceptor> customedInterceptors = new ArrayList<>(interceptors.size() + 1);
		customedInterceptors.addAll(interceptors);
		customedInterceptors.add(interceptor);
		
		loadBalancedRestTemplate.setInterceptors(customedInterceptors);
	}
	
	public SpringClientFactory springClientFactory() {
		SpringClientFactory factory = new SpringClientFactory();
		ArrayList<RibbonClientSpecification> list = new ArrayList<RibbonClientSpecification>(configurations);
		list.add(new RibbonClientSpecification("default.easytrans", new Class[]{RestEasyTransactionConfiguration.class}));
		factory.setConfigurations(list);
		return factory;
	}
	
	public static class RestEasyTransactionConfiguration{
		@Bean
		public IRule easyTransLoadBalanceRule(){
			return new StickyBestAvailableRule();
		}
	}
	
	public static class StickyBestAvailableRule extends BestAvailableRule{
		
		@Override
		public Server choose(Object key) {
			
			//使用一致性哈希进行分发
			//TODO 此处使用了GUAVA中简单的一致性哈希算法选择服务，但这里存在性能缺陷：当reachableServers中间某一个服务节点失效了
			//那么后续节点的一致性哈希结果将会不匹配，后续需要使用更完善的哈希环 加上 虚拟节点 的形式解决本问题
			List<Server> reachableServers = getLoadBalancer().getReachableServers();
			if(reachableServers != null && reachableServers.size() != 0){
				int serverSeq = Hashing.consistentHash(Thread.currentThread().getId(), reachableServers.size());
				return reachableServers.get(serverSeq);
			} else {
				return super.choose(key);
			}
		}
	}
	
	public RestTemplate getLoadBalancedRestTemplate() {
		return loadBalancedRestTemplate;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <P extends EasyTransRequest<R, ?>, R extends Serializable> R call(String appId, String busCode, String innerMethod, Map<String,Object> header, P params) {
		
		RestConsumerProperties restConsumerProperties = properties.getConsumer().get(appId);
		String context = RestRibbonEasyTransConstants.DEFAULT_URL_CONTEXT;
		if(restConsumerProperties!= null && restConsumerProperties.getContext() != null){
			context = restConsumerProperties.getContext();
		}
		
		Class<? extends EasyTransRequest> paramsClass = params.getClass();
		Class<?> resultClass = ReflectUtil.getResultClass(paramsClass);
		
		
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.set(RestRibbonEasyTransConstants.HttpHeaderKey.EASYTRANS_HEADER_KEY, encodeEasyTransHeader(header));
		HttpEntity<P> requestEntity = new HttpEntity<>(params, headers);
		
		ResponseEntity<?> exchangeResult = loadBalancedRestTemplate.exchange("http://" + appId + "/" + context + "/" + busCode + "/" + innerMethod, HttpMethod.POST, requestEntity, resultClass);
		if(!exchangeResult.getStatusCode().is2xxSuccessful()){
			throw new RuntimeException("远程请求发生错误:" + exchangeResult);
		}
		
		return (R) exchangeResult.getBody();
	}

	private String encodeEasyTransHeader(Map<String, Object> header) {
		return new String(Base64.getEncoder().encode(serializer.serialization(header)), StandardCharsets.ISO_8859_1);
	}



	@Override
	public <P extends EasyTransRequest<R, ?>, R extends Serializable> void callWithNoReturn(
			String appId, String busCode, String innerMethod, Map<String,Object> header, P params) {
		call(appId, busCode, innerMethod, header, params);
		return;
	}

}
