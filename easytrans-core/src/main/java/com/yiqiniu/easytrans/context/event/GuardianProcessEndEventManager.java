package com.yiqiniu.easytrans.context.event;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.protocol.TransactionId;

public class GuardianProcessEndEventManager {

	private LogProcessContext logCtx;
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	private List<GuardianProcessEndEventHandler> listeners = new ArrayList<GuardianProcessEndEventHandler>(); 
	
	public GuardianProcessEndEventManager(LogProcessContext logCtx) {
		super();
		this.logCtx = logCtx;
	}

	
	public void registerSemiLogEventListener(GuardianProcessEndEventHandler handler){
		listeners.add(handler);
	}
	
	public boolean publish(){
		TransactionId transactionId = logCtx.getTransactionId();
		for(GuardianProcessEndEventHandler handler:listeners){
			if(!handler.beforeProcessEnd(logCtx)){
				LOG.info("GuardianProcessEndEvent handler return false,appId:{},busCode,trxId:{}",transactionId.getAppId(),transactionId.getBusCode(),transactionId.getTrxId());
				return false;
			}
		}
		
		return true;
	}
}
