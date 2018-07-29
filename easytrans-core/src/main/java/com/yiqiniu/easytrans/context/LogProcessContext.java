package com.yiqiniu.easytrans.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.yiqiniu.easytrans.context.event.DemiLogEventHandler;
import com.yiqiniu.easytrans.context.event.DemiLogEventManager;
import com.yiqiniu.easytrans.context.event.GuardianProcessEndEventHandler;
import com.yiqiniu.easytrans.context.event.GuardianProcessEndEventManager;
import com.yiqiniu.easytrans.core.ExecuteCacheManager;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.log.LogCache;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.AfterCommit;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.protocol.TransactionId;


public class LogProcessContext{
	/**
	 * final transaction status,null for unknown,true for commit , false for rollback
	 */
	private Boolean finalMasterTransStatus;
	private LogCollection logCollection;
	
	private DemiLogEventManager demiLogManager;
	private GuardianProcessEndEventManager processEndManager;
	private ExecuteCacheManager executeManager;
	private LogCache logCache;
	private TransStatusLogger transStatusChecker;
	
	private TransactionLogWritter writer;
	private TransactionId transactionId;
	private Map<String,AtomicInteger> callSeqMap = new HashMap<>();
	

	private AtomicInteger atomicInger = new AtomicInteger(1);
	private ConcurrentHashMap<Object, Object> extendResourceMap = new ConcurrentHashMap<Object, Object>();
	
	public int getAndIncTransUniqueId(){
		return atomicInger.getAndIncrement();
	}
	
	
	
	public Map<String, AtomicInteger> getCallSeqMap() {
		return callSeqMap;
	}



	public ConcurrentHashMap<Object, Object> getExtendResourceMap() {
		return extendResourceMap;
	}

	/**
	 * for normal processing
	 * @param transStatusChecker 
	 */
	public LogProcessContext(String appId,String busCode,String trxId,TransactionLogWritter writer, TransStatusLogger transStatusChecker) {
		super();
		this.logCollection = new LogCollection(appId, busCode, trxId, new ArrayList<Content>(), new Date());
		transactionId = new TransactionId(appId, busCode, trxId);
		init(false, writer,transStatusChecker);
	}
	
	/**
	 * for crash recover
	 * @param masterTransCommited
	 * @param logCollection
	 * @param writer
	 */
	public LogProcessContext(LogCollection logCollection,TransactionLogWritter writer, TransStatusLogger transStatusChecker) {
		super();
		this.logCollection = logCollection;
		transactionId = new TransactionId(logCollection.getAppId(), logCollection.getBusCode(), logCollection.getTrxId());
		init(null, writer, transStatusChecker);
	}

	private void init(Boolean masterTransCommited, TransactionLogWritter writer, TransStatusLogger transStatusChecker) {
		this.finalMasterTransStatus = masterTransCommited;
		this.writer = writer;
		demiLogManager = new DemiLogEventManager(this);
		processEndManager = new GuardianProcessEndEventManager(this);
		executeManager = new ExecuteCacheManager(this);
		logCache = new LogCache(this);
		this.transStatusChecker = transStatusChecker;
	}
	
	public TransactionId getTransactionId(){
		return transactionId;
	}
	
	public LogCache getLogCache() {
		return logCache;
	}

	public TransactionLogWritter getWriter() {
		return writer;
	}

	public DemiLogEventManager getDemiLogManager() {
		return demiLogManager;
	}

	public GuardianProcessEndEventManager getProcessEndManager() {
		return processEndManager;
	}

	public void registerDemiLogEventListener(DemiLeftContent LeftContent,DemiLogEventHandler handler){
		demiLogManager.registerSemiLogEventListener(LeftContent, handler);
	}
	
	public void registerProcessEndEventListener(GuardianProcessEndEventHandler handler){
		processEndManager.registerSemiLogEventListener(handler);
	}
	
	/**
	 * set the final status of transaction
	 * @param status true for commit,false for roll back
	 */
	public void setFinalMasterTransStatus(Boolean status){
		finalMasterTransStatus = status;
	}
	
	/**
	 * status true for commit,false for roll back,null for unknown
	 * @return
	 */
	public Boolean getFinalMasterTransStatus() {
		if(finalMasterTransStatus == null){
			//check by logs first
			finalMasterTransStatus = checkTransStatusByLogs();
			
			//then check by API
			if(finalMasterTransStatus == null){
				finalMasterTransStatus = transStatusChecker.checkTransactionStatus(getLogCollection().getAppId(),getLogCollection().getBusCode(), getLogCollection().getTrxId());
			}
		}
		return finalMasterTransStatus;
	}

	private Boolean checkTransStatusByLogs() {
		for(Content c:getLogCollection().getOrderedContents()){
			
			if(isCommitedLog(c.getClass())){
				return true;//there is a committed log, so the transaction must be committed
			}
			
			if(isRollBackLog(c.getClass())){
				return false;//there is a roll back log, so the transaction must be rolled back
			}
		}
		return null;//can't tell
	}
	
	
	private ConcurrentHashMap<Class<?>, Boolean> mapCommitedLogContent = new ConcurrentHashMap<Class<?>, Boolean>(); 
	private boolean isCommitedLog(Class<? extends Content> contentClass){
		Boolean isCommitedLogContent = mapCommitedLogContent.get(contentClass);
		if(isCommitedLogContent == null){
			isCommitedLogContent = hasAnnotion(contentClass,AfterCommit.class);
			mapCommitedLogContent.put(contentClass, isCommitedLogContent);
		}
		return isCommitedLogContent;
	}
	
	private ConcurrentHashMap<Class<?>, Boolean> mapRollBackLogContent = new ConcurrentHashMap<Class<?>, Boolean>();
	private boolean isRollBackLog(Class<? extends Content> contentClass){
		Boolean isRollbackedLogContent = mapRollBackLogContent.get(contentClass);
		if(isRollbackedLogContent == null){
			isRollbackedLogContent = hasAnnotion(contentClass,AfterCommit.class);
			mapRollBackLogContent.put(contentClass, isRollbackedLogContent);
		}
		return isRollbackedLogContent;
	
	}

	private boolean hasAnnotion(Class<? extends Content> checkClass,Class<? extends Annotation> annotation) {
		if(checkClass.getAnnotation(annotation) == null){
			return false;
		}else{
			return true;
		}
	}

	public LogCollection getLogCollection() {
		return logCollection;
	}

	public ExecuteCacheManager getExecuteManager() {
		return executeManager;
	}
}