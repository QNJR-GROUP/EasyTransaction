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
import com.yiqiniu.easytrans.log.vo.tcc.PreTccCallContent;
import com.yiqiniu.easytrans.log.vo.tcc.TccCallCancelledContent;
import com.yiqiniu.easytrans.log.vo.tcc.TccCallConfirmedContent;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.tcc.TccMethod;
import com.yiqiniu.easytrans.util.ReflectUtil;

@RelativeInterface(TccMethod.class)
public class TccMethodExecutor implements EasyTransExecutor,LogProcessor,DemiLogEventHandler {

	private EasyTransSynchronizer transSynchronizer;
	private RemoteServiceCaller rpcClient;
	
	public TccMethodExecutor(EasyTransSynchronizer transSynchronizer, RemoteServiceCaller rpcClient) {
		super();
		this.transSynchronizer = transSynchronizer;
		this.rpcClient = rpcClient;
	}

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private static final String TRY_METHOD_NAME = "doTry";
	private static final String CONFIRM_METHOD_NAME = "doConfirm";
	private static final String CANCEL_METHOD_NAME = "doCancel";
	
	@Override
	public <P extends EasyTransRequest<R,E>,E extends EasyTransExecutor,R extends Serializable> Future<R> execute(final Integer callSeq, final P params) {
		final LogProcessContext logProcessContext = transSynchronizer.getLogProcessContext();
		Callable<R> callable = new Callable<R>() {
			@Override
			public R call() throws Exception {
				BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
				return (R) rpcClient.call(businessIdentifer.appId(), businessIdentifer.busCode(), callSeq, TRY_METHOD_NAME, params,logProcessContext);
			}
		};
		
		PreTccCallContent content = new PreTccCallContent();
		content.setParams(params);
		content.setCallSeq(callSeq);
		return transSynchronizer.executeMethod(callable, content);
	}

	@Override
	public boolean logProcess(LogProcessContext ctx, Content currentContent) {
		if(currentContent instanceof PreTccCallContent){
			PreTccCallContent preCallContent = (PreTccCallContent) currentContent;
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
		PreTccCallContent preCallContent = (PreTccCallContent) leftContent;
		EasyTransRequest<?,?> params = preCallContent.getParams();
		BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(params.getClass());
		if(logCtx.getFinalMasterTransStatus() == null){
			LOG.info("final trans status unknown,process later." + logCtx.getLogCollection());
			return false;//unknown,process later
		}else if(logCtx.getFinalMasterTransStatus()){
			//commit
			//execute confirm and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCallContent.getCallSeq(), CONFIRM_METHOD_NAME, preCallContent.getParams(),logCtx);
			TccCallConfirmedContent tccCallConfirmedContent = new TccCallConfirmedContent();
			tccCallConfirmedContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(tccCallConfirmedContent);
			return true;
		}else{
			//roll back
			//execute cancel and then write Log
			rpcClient.callWithNoReturn(businessIdentifer.appId(), businessIdentifer.busCode(), preCallContent.getCallSeq(), CANCEL_METHOD_NAME, preCallContent.getParams(),logCtx);
			TccCallCancelledContent tccCallCanceledContent = new TccCallCancelledContent();
			tccCallCanceledContent.setLeftDemiConentId(leftContent.getcId());
			logCtx.getLogCache().cacheLog(tccCallCanceledContent);
			return true;
		}
	}


}
