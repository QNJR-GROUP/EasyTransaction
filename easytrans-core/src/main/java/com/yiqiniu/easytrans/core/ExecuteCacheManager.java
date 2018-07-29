package com.yiqiniu.easytrans.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.Assert;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.log.vo.Content;

/**
 *	batch write logs and batch execute Compensable methods to enhance efficient
 */
public class ExecuteCacheManager {
	
	//Set a max thread count, if accumulate to fast will reject execute for efficient
	private static ExecutorService executor = new ThreadPoolExecutor(0, 2048, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

	private ConcurrentHashMap<Callable<?>,Object[]/*0 Result,1 Exception,2 Boolean(Executed?)*/> mapCallable = new ConcurrentHashMap<Callable<?>,Object[]/*1 Result,2 Exception*/>();
	
	private LogProcessContext logCtx;
	
	private Map<Callable<?>,Exception> mapErrorCalls = new HashMap<Callable<?>, Exception>();
	
	public Map<Callable<?>,Exception> getErrorCalls(){
		return new HashMap<Callable<?>, Exception>(mapErrorCalls);//copy to protect orign data
	}
	
	public ExecuteCacheManager(LogProcessContext logCtx){
		this.logCtx = logCtx;
	}
	
	public <T> Future<T> cacheCall(Callable<T> compensableCall,Content content){
		logCtx.getLogCache().cacheLogList(Arrays.asList(content));
		
		mapCallable.put(compensableCall,new Object[3]);
		CompensableCallerWrapper<T> compensableCallWrapper = new CompensableCallerWrapper<T>(compensableCall);
		Future<T> future = new ExecuteTriggerByGetFuture<T>(compensableCallWrapper);
		return future;
	}
	
	
	/**
	 * Use Combination Pattern to implement.
	 * 
	 * the callable method will be trigger by calling get() method
	 */
	private class ExecuteTriggerByGetFuture<T> implements Future<T>{
		
		private FutureTask<T> futureTask;
		
		public ExecuteTriggerByGetFuture(Callable<T> callable) {
			super();
			this.futureTask = new FutureTask<T>(callable);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			// implements later
			throw new RuntimeException("Not support operation");
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return futureTask.isDone();
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			futureTask.run();
			return futureTask.get();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			futureTask.run();
			return futureTask.get(timeout, unit);
		}
		
	}
	
	/**
	 * Wrap the compensableCall for efficient
	 * 
	 * @param <T>
	 */
	private class CompensableCallerWrapper<T> implements Callable<T>{
		
		private Callable<T> compensableCall;
		private long objCreateThread;
		
		public CompensableCallerWrapper(Callable<T> compensableCall) {
			super();
			this.compensableCall = compensableCall;
			objCreateThread = Thread.currentThread().getId();
		}

		@SuppressWarnings("unchecked")
		@Override
		public T call() throws Exception {
			Assert.isTrue(objCreateThread == Thread.currentThread().getId(),"It's not thread safe,do not run in other threads");
			
			//before compensable methods call,flush all the logs
			logCtx.getLogCache().flush(false);
			//batch execute all the cached compensable methods
			excuteCahcheMehods();
			//get the specific result of compensableCall 
			Object[] objects = mapCallable.get(compensableCall);
			
			if(objects[2] == null || objects[2].equals(false)){
				throw new RuntimeException("Unkonw Error,It should has value!");
			}
			
			if(objects[1] != null){
				throw (Exception)objects[1];
			}else{
				return (T)objects[0];
			}
		}
	}
	
	
	public void excuteCahcheMehods() {
		
		Map<Callable<?>,Future<?>> listFuture = new HashMap<Callable<?>,Future<?>>();
		
		for(Entry<Callable<?>, Object[]> entry :mapCallable.entrySet()){
			Object[] results = entry.getValue();
			if(results[2] == null || results[2].equals(false)){
				//did not execute,then execute
				Callable<?> caller = entry.getKey();
				listFuture.put(caller, executor.submit(caller));
			}
		}
		
		//get and place the results in map
		for(Entry<Callable<?>, Future<?>> entry:listFuture.entrySet()){
			Callable<?> caller = entry.getKey();
			Future<?> future = entry.getValue();
			try {
				mapCallable.put(caller, new Object[]{future.get(),null,true});
			} catch (InterruptedException e) {
				mapErrorCalls.put(caller, e);
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				mapErrorCalls.put(caller, e);
				mapCallable.put(caller, new Object[]{null,e.getCause(),true});
			}
		}
	}

}
