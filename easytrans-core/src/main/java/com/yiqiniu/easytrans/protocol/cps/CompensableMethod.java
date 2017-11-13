package com.yiqiniu.easytrans.protocol.cps;

import java.io.Serializable;

import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

/**
 * Methods here all should be Idempotent<br/>
 * Idempotent can implement by framework or business code<br/>
 * 
 * if business coder decide to implement by business code, the following situation should be consider:<br/>
 * <ul>
 * <li>all methods here must be Idempotent</li>
 * <li>compensation may execute before doCompensableBusiness,and you should not throw an exception but register it</li>
 * <li>after compensation executed,doCompensableBusiness should directory return and do nothing</li>
 * </ul>
 */
public interface CompensableMethod<P extends CompensableMethodRequest<R>, R extends Serializable> extends RpcBusinessProvider<P>{
	/**
	 * finish the the business code
	 * @param param
	 * @return
	 */
	@ExecuteOrder(doNotExecuteAfter = { "compensation" }, ifNotExecutedReturnDirectly = {}, isSynchronousMethod=true)
	R doCompensableBusiness(P param);
	/**
	 * roll back the business code in doCompensableBusiness
	 * @param param
	 */
	@ExecuteOrder(doNotExecuteAfter = {}, ifNotExecutedReturnDirectly = {"doCompensableBusiness"})
    void compensation(P param);
}
