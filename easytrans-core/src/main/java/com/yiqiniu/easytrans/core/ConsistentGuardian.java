package com.yiqiniu.easytrans.core;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.Content.ContentType;
import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;
import com.yiqiniu.easytrans.log.vo.LogCollection;

/**
 * Keep eventually consistent based on logs
 */
public class ConsistentGuardian {
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private TransStatusLogger transChecker;
	
	private Map<Class<?>,? extends LogProcessor> proccessorMap;
	
	private TransactionLogWritter writer;
	
	private boolean leastLogModel;
	
	
	public ConsistentGuardian(TransStatusLogger transChecker, Map<Class<?>,? extends LogProcessor> proccessorMap,
			TransactionLogWritter writer,boolean leastLogModel) {
		super();
		this.transChecker = transChecker;
		this.proccessorMap = proccessorMap;
		this.writer = writer;
		this.leastLogModel = leastLogModel;
	}

	/**
	 * eventually consistent processing for recover
	 * @param logCollection all the logs in the current time 
	 */
	public boolean process(LogCollection logCollection){
		return process(buldLogContextFromLog(logCollection));
	}

	public LogProcessContext buldLogContextFromLog(LogCollection logCollection) {
		return new LogProcessContext(logCollection,writer,transChecker);
	}
	
	/**
	 * eventually consistent processing
	 * @param logCtx
	 */
	public boolean process(LogProcessContext logCtx){
		
		Boolean currentTrxStatus = logCtx.getFinalMasterTransStatus();
		logCtx.getMasterTransactionStatusVotter().setTransactionStatus(currentTrxStatus);
		
		LogCollection logCollection = logCtx.getLogCollection();
		List<Content> orderedContents = logCollection.getOrderedContents();
		
		//依次调用LOG对应的processor里的preProcess
		//目前preProcess中其中一个任务就是确定主控事务状态，另外一个就是执行SAGA的 正向调用/TRY 等方法
		//若主控事务状态未知，则部分LOG对应的preProcess操作里将会投票表决主控事务状态，若有表示不能提交的话，则事务状态为回滚
		//根据preProcess的数据更新主控事务状态（主控事务状态确定，才能往下执行，）
		for(int i = 0;i < orderedContents.size() ; i++){
			Content content = orderedContents.get(i);
			//check log order
			Assert.isTrue(content.getcId() != null && content.getcId().equals(i + 1),"content list did not sort or contentId is null");
			
			Class<? extends LogProcessor> proccessorClass = ContentType.getById(content.getLogType()).getProccessorClass();
			if(proccessorClass == null){
				if(LOG.isDebugEnabled()){
					LOG.debug("processor not set,continue" + content);
				}
				continue;
			}
			
			LogProcessor processor = proccessorMap.get(proccessorClass);
			if(!processor.preLogProcess(logCtx, content)){
				LOG.warn( "log pre-Processor return false,end proccesing and retry later." + content);
				return false;
			}
		}
		
		// 事务状态未知，且本事务为主控事务，则更新主控事务状态
		if (currentTrxStatus == null
				&& MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY) == null) {
			boolean allowCommit = logCtx.getMasterTransactionStatusVotter().getCurrentVoteStatusCommited();
			int updateCount = transChecker.updateMasterTransactionStatus(logCtx.getTransactionId(),
					allowCommit ? TransactionStatus.COMMITTED : TransactionStatus.ROLLBACKED);
			
			//concurrent modify check 
			if(updateCount == 0) {
				throw new RuntimeException("can not find the trx,or the status of Transaction is not UNKOWN!");
			}
			
			logCtx.setFinalMasterTransStatus(allowCommit);
		}
		
		//依次调用LOG对应的Processor的logProcess
		//统计并发布SemiLog事件
		//发布ProcessEnd事件
		for(int i = 0;i < orderedContents.size() ; i++){
			
			Content content = orderedContents.get(i);
			//check log order
			Assert.isTrue(content.getcId() != null && content.getcId().equals(i + 1),"content list did not sort or contentId is null");
			
			if(content instanceof DemiLeftContent){
				logCtx.getDemiLogManager().registerLeftDemiLog((DemiLeftContent)content);
			}else if(content instanceof DemiRightContent){
				logCtx.getDemiLogManager().registerRightDemiLog((DemiRightContent)content);
			}
			
			Class<? extends LogProcessor> proccessorClass = ContentType.getById(content.getLogType()).getProccessorClass();
			if(proccessorClass == null){
				if(LOG.isDebugEnabled()){
					LOG.debug("processor not set,continue" + content);
				}
				continue;
			}
			
			LogProcessor processor = proccessorMap.get(proccessorClass);
			if(!processor.logProcess(logCtx, content)){
				LOG.warn( "log processor return false,end proccesing and retry later." + content);
				return false;
			}
		}

		if(!logCtx.getDemiLogManager().pubulishDemiLogEvent()){
			LOG.warn("DemiLogEvent Process return false,end proccesing and retry later." + logCollection);
		}
		
		if(!logCtx.getProcessEndManager().publish()){
			LOG.warn("End process return false,end proccesing and retry later." + logCollection);
		}
		
		//end and flush log
		if(leastLogModel){
			logCtx.getLogCache().clearCacheLogs();
		}
		logCtx.getLogCache().flush(true);
		return true;
	}
	
}
