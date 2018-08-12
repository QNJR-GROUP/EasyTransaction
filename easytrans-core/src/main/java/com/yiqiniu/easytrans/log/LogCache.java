package com.yiqiniu.easytrans.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.LogCollection;

public class LogCache {

	private LogProcessContext logCtx;
	
	public LogCache(LogProcessContext logCtx){
		this.logCtx = logCtx;
	}
	
	private List<Content> cachedContentList = new ArrayList<Content>();
	
	public void cacheLog(Content content){
		cacheLogList(Arrays.asList(content));
	}
	
	public synchronized void cacheLogList(List<Content> newCachedLogs){
		LogCollection logCollection = logCtx.getLogCollection();
		if(newCachedLogs != null){
			// nextId = flushedSize + cachedSize + 1
			int flushedSize = logCollection == null?0:logCollection.getOrderedContents().size();
			int nextId = flushedSize + cachedContentList.size() + 1;
			for(Content c:newCachedLogs){
				Assert.isTrue(c.getcId() == null,"contentId is set in this method, please keep it null");
				c.setcId(nextId++);
				cachedContentList.add(c);
			}
		}
	}
	
	public synchronized void clearCacheLogs(){
		if(!cachedContentList.isEmpty()){
			cachedContentList.clear();
		}
	}
	
	public synchronized void flush(boolean trxEnd){
		
		if(!trxEnd && cachedContentList.isEmpty()){
			return;
		}
		
		LogCollection logCollection = logCtx.getLogCollection();
		String appId = logCtx.getTransactionId().getAppId();
		String busCode = logCtx.getTransactionId().getBusCode();
		long trxId = logCtx.getTransactionId().getTrxId();
		
		logCtx.getWriter().appendTransLog(appId, busCode, trxId, cachedContentList, trxEnd);
		logCollection.getOrderedContents().addAll(cachedContentList);
		cachedContentList.clear();
	}
}
