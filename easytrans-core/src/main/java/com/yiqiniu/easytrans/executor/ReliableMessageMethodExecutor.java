package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.context.event.DemiLogEventHandler;
import com.yiqiniu.easytrans.core.EasyTransSynchronizer;
import com.yiqiniu.easytrans.core.LogProcessor;
import com.yiqiniu.easytrans.core.RemoteServiceCaller;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.msg.MessageRecordContent;
import com.yiqiniu.easytrans.log.vo.msg.MessageSentContent;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.protocol.msg.PublishResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublishResult;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class ReliableMessageMethodExecutor implements EasyTransExecutor,LogProcessor,DemiLogEventHandler {

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller publisher;
	
	
	public ReliableMessageMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller publisher) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.publisher = publisher;
	}

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	@SuppressWarnings("unchecked")
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer callSeq, final P params) {
	
		MessageRecordContent content = new MessageRecordContent();
		content.setParams(params);
		content.setCallSeq(callSeq);
		transSynchronizer.registerLog(content);
		final PublishResult result = new PublishResult();
		result.setMessageContentId(content.getcId());
		
		FutureTask<PublishResult> future = new FutureTask<PublishResult>(new Callable<PublishResult>() {
			@Override
			public PublishResult call() throws Exception {
				return result;
			}
		});
		future.run();
		
		return (Future<R>) future;
	}

	@Override
	public boolean logProcess(LogProcessContext ctx, Content currentContent) {
		if(currentContent instanceof MessageRecordContent){
			MessageRecordContent msgRecordContent = (MessageRecordContent) currentContent;
			//register DemiLogEvent
			ctx.getDemiLogManager().registerSemiLogEventListener(msgRecordContent, this);
		}
		return true;
	}

	@Override
	public boolean onMatch(LogProcessContext logCtx, Content leftContent, Content rightContent) {
		return true;// do nothig
	}

	@Override
	public boolean onDismatch(LogProcessContext logCtx, Content leftContent) {
		if(logCtx.getFinalMasterTransStatus() == null){
			LOG.info("final trans status unknown,process later." + logCtx.getLogCollection());
			return false;//unknown,process later
		}else if(logCtx.getFinalMasterTransStatus()){
			//commit
			//get Log message
			MessageRecordContent content = (MessageRecordContent) leftContent;
			EasyTransRequest<?,?> msg = content.getParams();
			TransactionId parentTrxId = logCtx.getTransactionId();
			//send message
			BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(msg.getClass());
			EasyTransMsgPublishResult send = publisher.publish(businessIdentifer.appId(), businessIdentifer.busCode(), content.getCallSeq(), getMessageId(content, parentTrxId), msg,logCtx);
			//writeLog
			MessageSentContent messageSentContent = new MessageSentContent();
			messageSentContent.setLeftDemiConentId(leftContent.getcId());
			messageSentContent.setRemoteMessageId(send.getMessageId());
			logCtx.getLogCache().cacheLog(messageSentContent);
			LOG.info("Reliable message sent:" + businessIdentifer);
			return true;
		}else{
			//rollback
			//do nothing
			return true;
		}
	}

	private String getMessageId(MessageRecordContent content,
			TransactionId parentTrxId) {
		return parentTrxId.getAppId()+"|"+parentTrxId.getBusCode()+"|"+parentTrxId.getTrxId()+"|"+content.getcId();
	}


}
