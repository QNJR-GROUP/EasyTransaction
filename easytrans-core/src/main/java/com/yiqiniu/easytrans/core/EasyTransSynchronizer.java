package com.yiqiniu.easytrans.core;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.protocol.TransactionId;

/**
 *	provide base instrument for TCC,MQ transaction support<br>
 *	interact with spring's TransactionSynchronizer
 */
public class EasyTransSynchronizer {
	
	private static final String LOG_PROCESS_CONTEXT = "LOG_PROCESS_CONTEXT";
	
	private TransactionLogWritter writer;
	private ConsistentGuardian consistentGuardian;
	private TransStatusLogger transStatusLogger;
	private String applicationName;
	
	private final Cache<TransactionId, ConcurrentLinkedQueue<LogProcessContext>> compensationLogContextCache = CacheBuilder.newBuilder()  
	        .initialCapacity(10)  
	        .concurrencyLevel(5)  
	        .expireAfterWrite(10, TimeUnit.SECONDS)  
	        .build();  
	
	public EasyTransSynchronizer(TransactionLogWritter writer, ConsistentGuardian consistentGuardian,
			TransStatusLogger transStatusLogger, String applicationName) {
		super();
		this.writer = writer;
		this.consistentGuardian = consistentGuardian;
		this.transStatusLogger = transStatusLogger;
		this.applicationName = applicationName;
	}

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
		
		//hook to TransactionSynchronizer
		TransactionSynchronizationManager.registerSynchronization(new TransactionHook());
		LogProcessContext logProcessContext = new LogProcessContext(applicationName, busCode, trxId, writer,transStatusLogger);
		TransactionSynchronizationManager.bindResource(LOG_PROCESS_CONTEXT,logProcessContext);
		
		//check whether is a parent transaction
		TransactionId pTrxId = getParentTransactionId();
		if(pTrxId == null){
			//if this transaction is roll back, this record is will disappear
			transStatusLogger.writeExecuteFlag(applicationName, busCode, trxId, null, null, null, TransactionStatus.COMMITTED);
		} else {
			//a transaction with parent transaction,it's status depends on parent
			//check whether the parent transaction status is determined
			Integer pTransStatus = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRANSACTION_STATUS);
			if(pTransStatus == null){
				pTransStatus = TransactionStatus.UNKNOWN;
			} 
			transStatusLogger.writeExecuteFlag(applicationName, busCode, trxId, pTrxId.getAppId(), pTrxId.getBusCode(), pTrxId.getTrxId(), pTransStatus);
		}
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

			// the parent's transaction status
			boolean hasParentTransaction = getParentTransactionId() != null;
			Integer pTrxStatus = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRANSACTION_STATUS);

			switch (status) {
			case STATUS_COMMITTED:
				if (hasParentTransaction) {
					// the status of a transaction with parent depends on parent
					// get parent transaction status
					if (pTrxStatus == null || pTrxStatus.equals(TransactionStatus.UNKNOWN)) {
						logProcessContext.setFinalMasterTransStatus(null);
					} else {
						if (pTrxStatus.equals(TransactionStatus.COMMITTED)) {
							logProcessContext.setFinalMasterTransStatus(true);
						} else {
							logProcessContext.setFinalMasterTransStatus(false);
						}
					}
				} else {
					logProcessContext.setFinalMasterTransStatus(true);
				}
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

			// unwritten logs are not necessary logs
			logProcessContext.getLogCache().clearCacheLogs();

			if (hasParentTransaction && (pTrxStatus == null || pTrxStatus.equals(TransactionStatus.UNKNOWN))) {
				// has parent transaction, the final transaction status is
				// unknown
				// pending the context a while and try to wait for the sync call
				// back to tell final status
				// this require the consumer support the sticky call
				pendCompensationLogContext(logProcessContext);
			} else {
				// asynchronous execute to enhance efficient
				executor.execute(getRunnableCompensation(logProcessContext));
			}

		}

	}
	
	private Runnable getRunnableCompensation(final LogProcessContext logProcessContext) {
		return new Runnable() {
			@Override
			public void run() {
				try{
					consistentGuardian.process(logProcessContext);
				}catch(Throwable e){
					LOG.error("consistentGuardian execute exception!",e);
				}
			}
		};
	}

	private TransactionId getParentTransactionId() {
		return MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
	}

	private void pendCompensationLogContext(LogProcessContext logProcessContext) {
		try {
			ConcurrentLinkedQueue<LogProcessContext>  set = compensationLogContextCache.get(getParentTransactionId(),new Callable<ConcurrentLinkedQueue<LogProcessContext>>(){
				@Override
				public ConcurrentLinkedQueue<LogProcessContext> call() throws Exception {
					return new ConcurrentLinkedQueue<LogProcessContext>();
				}
			});
			set.add(logProcessContext);
		} catch (ExecutionException e) {
			//it's OK to just log,this operation is just for efficient
			LOG.error("cache pending transaction error",e);
		}
	}
	
	public void cascadeExecuteCachedTransaction(TransactionId pTrxId,boolean trxStatus){
		ConcurrentLinkedQueue<LogProcessContext> queue = compensationLogContextCache.getIfPresent(pTrxId);
		if(queue == null) {
			return ;
		}
		
		for(LogProcessContext logContext = queue.poll(); logContext != null; logContext = queue.poll()){
			compensationLogContextCache.invalidate(pTrxId);
			logContext.setFinalMasterTransStatus(trxStatus);
			//asynchronous execute to enhance efficient
			executor.execute(getRunnableCompensation(logContext));
		}
	}
	
}
