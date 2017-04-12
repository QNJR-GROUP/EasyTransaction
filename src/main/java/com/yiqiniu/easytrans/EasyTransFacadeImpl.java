package com.yiqiniu.easytrans;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;

import com.yiqiniu.easytrans.executor.EasyTransExecutor;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class EasyTransFacadeImpl implements EasyTransFacade{

	@Resource
	private ApplicationContext ctx;
	
	private ConcurrentHashMap<Class<?>,EasyTransExecutor> mapExecutors = new ConcurrentHashMap<Class<?>, EasyTransExecutor>();
	
	@Resource
	private EasyTransSynchronizer synchronizer;
	
	private EasyTransExecutor getExecutor(@SuppressWarnings("rawtypes") Class<? extends EasyTransRequest> clazz){
		
		EasyTransExecutor easyTransExecutor = mapExecutors.get(clazz);
		if(easyTransExecutor == null){
			Class<?> executorClazz = findEasyTransExecutor(clazz);
			easyTransExecutor = (EasyTransExecutor) ctx.getBean(executorClazz);
			mapExecutors.put(clazz,easyTransExecutor);
		}
		return easyTransExecutor;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Class<?> findEasyTransExecutor(Class clazz) {
		List<Class<?>> typeArguments = ReflectUtil.getTypeArguments(EasyTransRequest.class, clazz);
		
		if(typeArguments != null && typeArguments.size() >= 2){
			return (Class<?>) typeArguments.get(1);
		}else{
			return null;
		}
	}
	
	public void startEasyTrans(String busCode,String trxId){
		synchronizer.startSoftTrans(busCode, trxId);
	}

	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor, R extends Serializable> Future<R> execute(P params){
		EasyTransExecutor e = getExecutor(params.getClass());
		return e.execute(params);
	}

}
