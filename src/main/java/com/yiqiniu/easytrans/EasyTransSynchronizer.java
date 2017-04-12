package com.yiqiniu.easytrans;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.yiqiniu.easytrans.config.EasyTransConifg;
import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.Content;

/**
 *	provide base instrument for TCC,MQ transaction support<br>
 *	interact with spring's TransactionSynchronizer
 */
public class EasyTransSynchronizer {
	
	private static final String LOG_PROCESS_CONTEXT = "LOG_PROCESS_CONTEXT";

	@Resource
	private EasyTransConifg config;
	
	@Resource
	private TransactionLogWritter writer;
	
	@Resource
	private ConsistentGuardian consistentGuardian;
	
	@Resource
	private TransStatusLogger transStatusLogger;
	
	private ExecutorService executor = Executors.newCachedThreadPool();
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * 需要执行的方法
	 * @param call
	 * @param content
	 * @return
	 */
	public <T> Future<T> executeMethod(Callable<T> call,Content content){
		LogProcessContext logProcessContext = getLogProcessContext();
		return logProcessContext.getExecuteManager().cacheCall(call, content);
	}
	
	/**
	 * create LogContext.<br>
	 * pass in busCode and trxId will help about the execute efficient and the debug and statistics<br>
	 * so I demand to pass in busCode and trxId to generate a global unique bus key 
	 * @param busCode the unique busCode in AppId
	 * @param trxId the unique trxId in appId+busCode
	 */
	public void startSoftTrans(String busCode,String trxId){
		
		Assert.notNull(busCode);
		Assert.notNull(trxId);
		
		//hook to TransactionSynchronizer
		TransactionSynchronizationManager.registerSynchronization(new TransactionHook());
		LogProcessContext logProcessContext = new LogProcessContext(config.getAppId(), busCode, trxId, writer,transStatusLogger);
		TransactionSynchronizationManager.bindResource(LOG_PROCESS_CONTEXT,logProcessContext);
		transStatusLogger.writeExecuteFlag(config.getAppId(), busCode, trxId);
	}

	public void registerLog(Content content){
		LogProcessContext logProcessContext = getLogProcessContext();
		logProcessContext.getLogCache().cacheLog(content);
	}

	/**
	 * get log context
	 * @return
	 */
	public LogProcessContext getLogProcessContext() {
		LogProcessContext logProcessContext = (LogProcessContext) TransactionSynchronizationManager.getResource(LOG_PROCESS_CONTEXT);
		if(logProcessContext == null){
			throw new RuntimeException("please call TransController.startSoftTrans() before executeMethods!");
		}
		return logProcessContext;
	}
	
	private void unbindLogProcessContext(){
		TransactionSynchronizationManager.unbindResource(LOG_PROCESS_CONTEXT);
	}
	
	private class TransactionHook extends TransactionSynchronizationAdapter{

		@Override
		public void beforeCommit(boolean readOnly) {
			//flush all the logs and execute all the compensable methods before commit
			LogProcessContext logProcessContext = getLogProcessContext();
			logProcessContext.getLogCache().flush(false);
			logProcessContext.getExecuteManager().excuteCahcheMehods();
			Map<Callable<?>, Exception> errorCalls = logProcessContext.getExecuteManager().getErrorCalls();
			if(errorCalls.size() != 0){
				Entry<Callable<?>, Exception> next = errorCalls.entrySet().iterator().next();
				throw new RuntimeException("Exist compensable method call Exception,rollback now...",next.getValue());
			}
		}
		
		
		@Override
		public void afterCompletion(int status) {
			final LogProcessContext logProcessContext = getLogProcessContext();
			unbindLogProcessContext();
			
			switch(status){
				case STATUS_COMMITTED:
					logProcessContext.setFinalMasterTransStatus(true);
					break;
				case STATUS_ROLLED_BACK:
					logProcessContext.setFinalMasterTransStatus(false);
					break;
				case STATUS_UNKNOWN:
					logProcessContext.setFinalMasterTransStatus(null);
					break;
				default:
					throw new RuntimeException("Unkonw Status!");
			}
			
			//unwritten logs are not necessary logs
			logProcessContext.getLogCache().clearCacheLogs();
			
			//asynchronous execute to enhance efficient
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try{
						consistentGuardian.process(logProcessContext);
					}catch(Throwable e){
						LOG.error("consistentGuardian execute exception!",e);
					}
				}
			});
		}
	}
	
}
