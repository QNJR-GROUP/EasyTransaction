package com.yiqiniu.easytrans.core;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.datasource.TransStatusLogger;
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
	
	
	public ConsistentGuardian(TransStatusLogger transChecker, Map<Class<?>,? extends LogProcessor> proccessorMap,
			TransactionLogWritter writer) {
		super();
		this.transChecker = transChecker;
		this.proccessorMap = proccessorMap;
		this.writer = writer;
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
		
		LogCollection logCollection = logCtx.getLogCollection();

		//依次调用LOG对应的Processor
		//统计并发布SemiLog事件
		//发布ProcessEnd事件
		List<Content> orderedContents = logCollection.getOrderedContents();
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
		logCtx.getLogCache().flush(true);
		return true;
	}
	
}
