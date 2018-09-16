package com.yiqiniu.easytrans.util;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Future;

import org.springframework.beans.BeanUtils;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.executor.EasyTransExecutor;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.tcc.TccMethodRequest;

public class CallWrapUtil {
	
	private EasyTransFacade facade;
	
	public CallWrapUtil(EasyTransFacade facade) {
		this.facade = facade;
	}
	
	public static class Result implements Serializable{
		private static final long serialVersionUID = 1L;
	}
	
	public static class Request{
		
	}
	
	public static interface TestInterface{
		Result tccCall(Request req);
	}
	
	
	
	@BusinessIdentifer(appId="testA",busCode="business")
	public static class RequestConfig implements TccMethodRequest<Result>{
		private static final long serialVersionUID = 1L;
		
	}
	

	@SuppressWarnings("unchecked")
	public <T,R extends Serializable,E extends EasyTransExecutor> T createTransactionCallInstance(Class<T> transactionApiClass, Class<? extends EasyTransRequest<R,E>> cfgClass) {
		
		//check whether class T has only one method and that method only has one parameter
		Method[] declaredMethods = transactionApiClass.getDeclaredMethods();
		if(declaredMethods.length != 1) {
			throw new RuntimeException("transactionApiClass must contains only one method!");
		}
		Method proxyMethod = declaredMethods[0];
		Class<?> returnType = proxyMethod.getReturnType();
		
		
		//create proxy that delegates to EasyTransFacade
		Object instance = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{transactionApiClass}, new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				
				if(!method.getName().equals(proxyMethod.getName())) {
					//object method, delegates to object
					return method.invoke(this, args);
				}
				
				if(args == null || args.length != 1) {
					throw new RuntimeException("Only allow one param in this method");
				}
				Object arg = args[0];
				EasyTransRequest<R, E> etRequest = cfgClass.newInstance();
				
				BeanUtils.copyProperties(arg, etRequest);
			
				Future<R> execute = facade.execute(etRequest);
				
				if(Future.class == returnType) {
					return execute;
				}
				
				if(void.class == returnType || Void.class == returnType) {
					return null;
				}
				
				return execute.get();
			}
		});
		
		//check whether return class is Future class 
		
		return (T) instance;
	}
	
	
}
