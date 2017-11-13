package com.yiqiniu.easytrans.core;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContext;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.executor.EasyTransExecutor;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class EasyTransFacadeImpl implements EasyTransFacade{

	private ApplicationContext ctx;
	private EasyTransSynchronizer synchronizer;

	
	public EasyTransFacadeImpl(ApplicationContext ctx, EasyTransSynchronizer synchronizer) {
		super();
		this.ctx = ctx;
		this.synchronizer = synchronizer;
	}

	private ConcurrentHashMap<Class<?>,EasyTransExecutor> mapExecutors = new ConcurrentHashMap<Class<?>, EasyTransExecutor>();
	
	
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
		BusinessIdentifer businessIdentifer = params.getClass().getAnnotation(BusinessIdentifer.class);
		EasyTransExecutor e = getExecutor(params.getClass());
		LogProcessContext logProcessContext = synchronizer.getLogProcessContext();
		Map<String, AtomicInteger> callSeqMap = logProcessContext.getCallSeqMap();
		String callSeqKey = businessIdentifer.appId() +  com.yiqiniu.easytrans.core.EasytransConstant.EscapeChar + businessIdentifer.busCode(); 
		AtomicInteger callSeq = callSeqMap.get(callSeqKey);
		if(callSeq == null){
			callSeq = new AtomicInteger(0);
			callSeqMap.put(callSeqKey, callSeq);
		}
		
		return e.execute(callSeq.incrementAndGet(), params);
	}

}
