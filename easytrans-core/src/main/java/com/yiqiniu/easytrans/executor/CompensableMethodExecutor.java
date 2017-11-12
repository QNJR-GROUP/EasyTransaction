package com.yiqiniu.easytrans.executor;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.context.event.DemiLogEventHandler;
import com.yiqiniu.easytrans.core.EasyTransSynchronizer;
import com.yiqiniu.easytrans.core.LogProcessor;
import com.yiqiniu.easytrans.core.RemoteServiceCaller;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.compensable.CompensatedContent;
import com.yiqiniu.easytrans.log.vo.compensable.PreCompensableCallContent;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.cps.CompensableMethod;
import com.yiqiniu.easytrans.util.ReflectUtil;

@RelativeInterface(CompensableMethod.class)
public class CompensableMethodExecutor implements EasyTransExecutor,LogProcessor,DemiLogEventHandler {

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller rpcClient;
	
	public CompensableMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.rpcClient = rpcClient;
	}
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private static final String COMPENSABLE_BUSINESS_METHOD_NAME = "doCompensableBusiness";
	private static final String COMPENSATION_METHOD_NAME = "compensation";
	
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer sameBusinessCallSeq, final P params) {
		final LogProcessContext logProcessContext = transSynchronizer.getLogProcessContext();
		Callable<R> callable = new Callable<R>() {
			@Override
			public R call() throws Exception {
				BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
				return (R) rpcClient.call(businessIdentifer.appId(), businessIdentifer.busCode(),  sameBusinessCallSeq, COMPENSABLE_BUSINESS_METHOD_NAME, params,logProcessContext);
			}
		};
		
		PreCompensableCallContent content = new PreCompensableCallContent();
		content.setCallSeq(sameBusinessCallSeq);
		content.setParams(params);
		
		return transSynchronizer.executeMethod(callable, content);
	}

	@Override
	public boolean logProcess(LogProcessContext ctx, Content currentContent) {
		if(currentContent instanceof PreCompensableCallContent){
			PreCompensableCallContent preCallContent = (PreCompensableCallContent) currentContent;
			//register DemiLogEvent
			ctx.getDemiLogManager().registerSemiLogEventListener(preCallContent, this);
		}
		return true;
	}

	@Override
	public boolean onMatch(LogProcessContext logCtx, Content leftContent, Content rightContent) {
		return true;// do nothing
	}

	@Override
	public boolean onDismatch(LogProcessContext logCtx, Content leftContent) {
		PreCompensableCallContent preCpsContent = (PreCompensableCallContent) leftContent;
		EasyTransRequest<?,?> params = preCpsContent.getParams();
		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
		
		if(logCtx.getFinalMasterTransStatus() == null){
			LOG.info("final trans status unknown,process later." + logCtx.getLogCollection());
			return false;
		}else if(logCtx.getFinalMasterTransStatus()){
			//commit
			//do nothing
			return true;
		}else{
			//roll back
			//execute compensation and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCpsContent.getCallSeq(), COMPENSATION_METHOD_NAME, preCpsContent.getParams(),logCtx);
			CompensatedContent compensatedContent = new CompensatedContent();
			compensatedContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(compensatedContent);
			LOG.info("Compensable method executed:" + businessIdentifer);
			return true;
		}
	}


}
