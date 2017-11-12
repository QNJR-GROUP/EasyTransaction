package com.yiqiniu.easytrans.rpc.impl.dubbo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.yiqiniu.easytrans.filter.EasyTransFilter;
import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.filter.EasyTransResult;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.util.ReflectUtil;

@Activate(group = "provider")
public class DubboEasyTransFilter implements Filter{
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		DubboEasyTransRpcProviderImpl instance = DubboEasyTransRpcProviderImpl.getInstance();
		if(checkEasyTransRequest(invocation)){
			EasyTransRequest<?, ?> easyTransRequest = (EasyTransRequest<?, ?>) ((Object[])invocation.getArguments()[2])[0];
			@SuppressWarnings("unchecked")
			Map<String,Object> header = (Map<String,Object>) ((Object[])invocation.getArguments()[2])[1];
			BusinessIdentifer businessIdentifer = ReflectUtil.getBusinessIdentifer(easyTransRequest.getClass());
			EasyTransFilterChain filterChain = instance.getFilterChainFactory().getFilterChainByFilters(businessIdentifer.appId(), businessIdentifer.busCode(), (String)(invocation.getArguments()[0]),instance.getFilters());
			
			filterChain.addFilter(new EasyTransFilterAdapter(invoker, invocation));
			
			EasyTransResult result;
			try {
				result = filterChain.invokeFilterChain(header,easyTransRequest);
			} catch (Exception e) {
				LOG.error("RPC EasyTrans FilterChain execute Error!",e);
				throw e;
			}
			
			if(result.hasException()){
				return new RpcResult(result.getException());
			}else{
				return new RpcResult(result.getValue());
			}
		}else{
			return invoker.invoke(invocation);
		}
	}
	
	private boolean checkEasyTransRequest(Invocation invocation) {
		if(invocation.getArguments().length == 3){
			Object param3 = invocation.getArguments()[2];
			if(param3 != null && param3.getClass().isArray()){
				Object[] array = (Object[]) param3;
				if(array.length == 2 && array[0] instanceof EasyTransRequest && array[1] instanceof Map){
					return true;
				}
			}
		}
		return false;
	}
	
	private static class EasyTransFilterAdapter implements EasyTransFilter{
		
		Invoker<?> localInvoker;
		Invocation localInvocation;
		
		public EasyTransFilterAdapter(Invoker<?> localInvoker,
				Invocation localInvocation) {
			super();
			this.localInvoker = localInvoker;
			this.localInvocation = localInvocation;
		}

		@Override
		public EasyTransResult invoke(EasyTransFilterChain filterChain, Map<String,Object> header,	EasyTransRequest<?, ?> request) {
			
			Result dubboInvoke = null;
			if(localInvocation instanceof RpcInvocation){
				Object[] arguments = localInvocation.getArguments();
				Object[] argReq = (Object[]) arguments[2];
				argReq[0] = request;
				dubboInvoke = localInvoker.invoke(localInvocation);
			}else{
				throw new RuntimeException("Do not support,please extend it");
			}
			
			EasyTransResult easyTransResult = new EasyTransResult();
			easyTransResult.setValue(dubboInvoke.getValue());
			easyTransResult.setException(dubboInvoke.getException());
			
			return easyTransResult;
		}
	}
	

}