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
import com.yiqiniu.easytrans.log.vo.fescar.FescarAtCallCommitedContent;
import com.yiqiniu.easytrans.log.vo.fescar.FescarAtCallRollbackedContent;
import com.yiqiniu.easytrans.log.vo.fescar.FescarAtPreCallContent;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.autocps.AutoCpsMethod;
import com.yiqiniu.easytrans.util.ReflectUtil;

@RelativeInterface(AutoCpsMethod.class)
public class AutoCpsMethodExecutor implements EasyTransExecutor,LogProcessor,DemiLogEventHandler {

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller rpcClient;
	
	public AutoCpsMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.rpcClient = rpcClient;
	}

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer callSeq, final P params) {
		final LogProcessContext logProcessContext = transSynchronizer.getLogProcessContext();
		Callable<R> callable = new Callable<R>() {
			@Override
			public R call() throws Exception {
				BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
				return (R) rpcClient.call(businessIdentifer.appId(), businessIdentifer.busCode(), callSeq, AutoCpsMethod.DO_AUTO_CPS_BUSINESS, params,logProcessContext);
			}
		};
		
		FescarAtPreCallContent content = new FescarAtPreCallContent();
		content.setParams(params);
		content.setCallSeq(callSeq);
		return transSynchronizer.executeMethod(callable, content);
	}

	@Override
	public boolean logProcess(LogProcessContext ctx, Content currentContent) {
		if(currentContent instanceof FescarAtPreCallContent){
		    FescarAtPreCallContent preCallContent = (FescarAtPreCallContent) currentContent;
			//register DemiLogEvent
			ctx.getDemiLogManager().registerSemiLogEventListener(preCallContent, this);
		}
		return true;
	}

	@Override
	public boolean onMatch(LogProcessContext logCtx, Content leftContent, Content rightContent) {
		return true;// do nothig
	}

	@Override
	public boolean onDismatch(LogProcessContext logCtx, Content leftContent) {
	    FescarAtPreCallContent preCallContent = (FescarAtPreCallContent) leftContent;
		EasyTransRequest<?,?> params = preCallContent.getParams();
		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
		if(logCtx.getFinalMasterTransStatus() == null){
			LOG.info("final trans status unknown,process later." + logCtx.getLogCollection());
			return false;//unknown,process later
		}else if(logCtx.getFinalMasterTransStatus()){
			//commit
			//execute confirm and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCallContent.getCallSeq(), AutoCpsMethod.DO_AUTO_CPS_COMMIT, preCallContent.getParams(),logCtx);
			FescarAtCallCommitedContent committedContent = new FescarAtCallCommitedContent();
			committedContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(committedContent);
			return true;
		}else{
			//roll back
			//execute cancel and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCallContent.getCallSeq(), AutoCpsMethod.DO_AUTO_CPS_ROLLBACK, preCallContent.getParams(),logCtx);
			FescarAtCallRollbackedContent rollbackedContent = new FescarAtCallRollbackedContent();
			rollbackedContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(rollbackedContent);
			return true;
		}
	}

	@Override
	public boolean preLogProcess(LogProcessContext ctx, Content currentContent) {
		return true;
	}


}
