package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
import com.yiqiniu.easytrans.log.vo.aft.AfterTransCallRegisterContent;
import com.yiqiniu.easytrans.log.vo.aft.AfterTransCalledContent;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.aft.AfterMasterTransMethod;
import com.yiqiniu.easytrans.util.FutureAdapter;
import com.yiqiniu.easytrans.util.ReflectUtil;

@RelativeInterface(AfterMasterTransMethod.class)
public class AfterTransMethodExecutor implements EasyTransExecutor,LogProcessor,DemiLogEventHandler {

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller rpcClient;
	
	public AfterTransMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.rpcClient = rpcClient;
	}


	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private static final String AFTER_TRANS_METHOD_NAME = "afterTransaction";
	
	private static final String AFTER_TRANS_METHOD_FUTURE_PREFIX = "ATMF";
	
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer sameBusinessCallSeq, final P params) {
		final AfterTransCallRegisterContent content = new AfterTransCallRegisterContent();
		content.setParams(params);
		content.setCallSeq(sameBusinessCallSeq);
		transSynchronizer.registerLog(content);
		
		LogProcessContext logProcessContext = transSynchronizer.getLogProcessContext();
		FutureProxy<R> future = new FutureProxy<R>(logProcessContext);
		Object orign = logProcessContext.getExtendResourceMap().put(getFutureKey(content), future);
		if(orign != null){
			throw new RuntimeException("Unkown Exception:" + AFTER_TRANS_METHOD_FUTURE_PREFIX + content.getcId());
		}
		return future;
	}

	@Override
	public boolean logProcess(LogProcessContext ctx, Content currentContent) {
		if(currentContent instanceof AfterTransCallRegisterContent){
			AfterTransCallRegisterContent afterTransCallContent = (AfterTransCallRegisterContent) currentContent;
			//register DemiLogEvent
			ctx.getDemiLogManager().registerSemiLogEventListener(afterTransCallContent, this);
		}
		return true;
	}

	@Override
	public boolean onMatch(LogProcessContext logCtx, Content leftContent, Content rightContent) {
		return true;// do nothig
	}

	@Override
	public boolean onDismatch(final LogProcessContext logCtx, Content leftContent) {
		final AfterTransCallRegisterContent afterTransCallContent = (AfterTransCallRegisterContent) leftContent;
		final EasyTransRequest<?,?> params = afterTransCallContent.getParams();
		final BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
		
		if(logCtx.getFinalMasterTransStatus() == null){
			LOG.info("final trans status unknown,process later." + logCtx.getLogCollection());
			return false;
		}else if(logCtx.getFinalMasterTransStatus()){
			//commit
			//execute the register method
			FutureTask<Object> futureTask = new FutureTask<Object>(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return rpcClient.call(businessIdentifer.appId(), businessIdentifer.busCode(), afterTransCallContent.getCallSeq(), AFTER_TRANS_METHOD_NAME, afterTransCallContent.getParams(),logCtx);
				}
			});
			
			futureTask.run();//get result in this thread
			try {
				futureTask.get();
				FutureProxy<?> object = (FutureProxy<?>) logCtx.getExtendResourceMap().get(getFutureKey(afterTransCallContent));
				if(object != null){
					object.setResult(futureTask);
				}
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("After transaction method execute ERROR",e);
				return false;//try later
			}
			
			AfterTransCalledContent compensatedContent = new AfterTransCalledContent();
			compensatedContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(compensatedContent);
			LOG.info("After transaction method executed:" + businessIdentifer);
			return true;
		}else{
			//roll back
			//do nothing
			return true;
		}
	}

	private String getFutureKey(
			final AfterTransCallRegisterContent afterTransCallContent) {
		return AFTER_TRANS_METHOD_FUTURE_PREFIX + afterTransCallContent.getcId();
	}
	
	
	private class FutureProxy<R> extends FutureAdapter<R> {
		
		private LogProcessContext logContext;
		
		public FutureProxy(LogProcessContext logContext){
			this.logContext = logContext;
		}
		
		private volatile Future<R> result;
		
		@SuppressWarnings("unchecked")
		public void setResult(Future<Object> result) {
			this.result = (Future<R>) result;
		}

		@Override
		public R get() throws InterruptedException, ExecutionException {
			
			Boolean finalMasterTransStatus = logContext.getFinalMasterTransStatus();
			if(finalMasterTransStatus == null || !finalMasterTransStatus){
				throw new RuntimeException("please get the result after the transaction commited");
			}
			
			if(result == null){
				throw new RuntimeException("Unknow Error!");
			}
			
			return result.get();
		}
	}


	@Override
	public boolean preLogProcess(LogProcessContext ctx, Content currentContent) {
		return true;
	};


}

