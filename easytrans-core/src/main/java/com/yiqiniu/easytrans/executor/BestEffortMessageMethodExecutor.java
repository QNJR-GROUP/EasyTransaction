package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.context.event.GuardianProcessEndEventHandler;
import com.yiqiniu.easytrans.core.EasyTransSynchronizer;
import com.yiqiniu.easytrans.core.RemoteServiceCaller;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.protocol.msg.PublishResult;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class BestEffortMessageMethodExecutor implements EasyTransExecutor{

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller publisher;

	public BestEffortMessageMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller publisher) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.publisher = publisher;
	}

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	@SuppressWarnings("unchecked")
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer sameBusinessCallSeq, final P params) {
		FutureTask<PublishResult> future = new FutureTask<PublishResult>(new Callable<PublishResult>() {
			@Override
			public PublishResult call() throws Exception {
				return new PublishResult();//do nothing
			}
		});
		future.run();
		
		
		final LogProcessContext logProcessContext = transSynchronizer.getLogProcessContext();
		
		//sent message after transaction commit
		logProcessContext.registerProcessEndEventListener(new GuardianProcessEndEventHandler() {
			@Override
			public boolean beforeProcessEnd(LogProcessContext logContext) {
				if(logProcessContext.getFinalMasterTransStatus()){
					BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
					String messageId = getMessageId("M" + logProcessContext.getAndIncTransUniqueId(), logContext.getTransactionId());
					publisher.publish(businessIdentifer.appId(), businessIdentifer.busCode(), sameBusinessCallSeq, messageId, params,logProcessContext);
					LOG.info("Best effort message sent." + messageId);
				}
				return true;
			}
		});
		
		return (Future<R>) future;
	}

	private String getMessageId(String innerId, TransactionId parentTrxId) {
		return parentTrxId.getAppId()+"|"+parentTrxId.getBusCode()+"|"+parentTrxId.getTrxId()+"|"+innerId;
	}


}

