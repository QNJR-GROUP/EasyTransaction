package com.yiqiniu.easytrans.context.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.DemiLeftContent;
import com.yiqiniu.easytrans.log.vo.DemiRightContent;
import com.yiqiniu.easytrans.log.vo.LogCollection;

public class DemiLogEventManager {
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());

	private LogProcessContext logCtx;
	
	public DemiLogEventManager(LogProcessContext logCtx) {
		super();
		this.logCtx = logCtx;
	}

	private Map<Content/*LeftDemi*/,Content/*the matche Right Content*/> mapMatchResult = new HashMap<Content,Content>();
	private Map<Content/*LeftDemi*/,List<DemiLogEventHandler>> mapListener = new HashMap<Content/*contentId*/,List<DemiLogEventHandler>>();
	
	
	public void registerSemiLogEventListener(Content content,DemiLogEventHandler handler){
		List<DemiLogEventHandler> list = mapListener.get(content);
		if(list == null){
			list = new ArrayList<DemiLogEventHandler>();
			mapListener.put(content, list);
		}
		list.add(handler);
	}
	
	public void registerLeftDemiLog(DemiLeftContent content){
		mapMatchResult.put(content, null);
	}
	
	public void registerRightDemiLog(DemiRightContent content){
		Integer leftDemiConentId = content.getLeftDemiConentId();
		Content leftContent = logCtx.getLogCollection().getOrderedContents().get(leftDemiConentId - 1);
		if(!leftContent.getcId().equals(leftDemiConentId)){
			throw new RuntimeException("Content did not sort correctly" + logCtx.getLogCollection().getOrderedContents()) ;
		}

		mapMatchResult.put(leftContent, content);
	}
	
	public boolean pubulishDemiLogEvent(){
		LogCollection logCollection = logCtx.getLogCollection();
		for(Entry<Content, Content> entry:mapMatchResult.entrySet()){
			Content leftContent = entry.getKey();
			Content rightContent = entry.getValue();
			List<DemiLogEventHandler> list = mapListener.get(leftContent);
			if(rightContent != null){
				for(DemiLogEventHandler handler:list){
					if(handler != null){
						if(!handler.onMatch(logCtx,leftContent,rightContent)){
							LOG.info("DemiLogEvent handler return false at onMatch,appId:{},trxId:{}handler:{}",logCollection.getAppId(),logCollection.getTrxId(),handler);
							return false;
						}
					}
				}
			}else{
				for(DemiLogEventHandler handler:list){
					if(handler != null){
						if(!handler.onDismatch(logCtx,leftContent)){
							LOG.info("DemiLogEvent handler return false at onDismatch,appId:{},trxId:{}handler:{}",logCollection.getAppId(),logCollection.getTrxId(),handler);
							return false;
						}
					}
				}
			}
			
		}
		
		return true;
	}


}
