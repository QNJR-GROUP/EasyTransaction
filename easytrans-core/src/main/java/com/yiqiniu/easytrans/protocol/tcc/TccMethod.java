package com.yiqiniu.easytrans.protocol.tcc;

import java.io.Serializable;

import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

/**
 * TCC for Try-Confirm-Cancel<br/>
 * <br/>
 * Methods here all should be Idempotent<br/>
 * Idempotent can implement by framework or business code<br/>
 * 
 * if business coder decide to implement by business code, the following situation should be consider:<br/>
 * <ul>
 * <li>all methods here must be Idempotent</li>
 * <li>doCancel may execute before doTry,and you should not throw an exception but register it</li>
 * <li>after doCancel executed,doTry should directory return and do nothing</li>
 * </ul>
 * @param <R>
 */
public interface TccMethod<P extends TccMethodRequest<R>, R  extends Serializable> extends RpcBusinessProvider<P> {
    
	/**
	 * reserve the resources that will be used in doConifirm(),so doConfirm() is always practicable in business
	 * @param param
	 * @return
	 */
	@ExecuteOrder(doNotExecuteAfter = { "doConfirm", "doCancel" }, ifNotExecutedReturnDirectly = {}, isSynchronousMethod=true)
	@MethodTransactionStatus(TransactionStatus.UNKNOWN)
	R doTry(P param);
	
	/**
	 * consume the resources reserved in doTry and finish the transaction
	 * @param param
	 */
	@MethodTransactionStatus(TransactionStatus.COMMITTED)
	void doConfirm(P param);
	
	/**
	 * release the resources reserved in doTry so the resources can be use by other transactions
	 * @param param
	 */
	@ExecuteOrder(doNotExecuteAfter = {}, ifNotExecutedReturnDirectly = { "doTry" })
	@MethodTransactionStatus(TransactionStatus.ROLLBACKED)
	void doCancel(P param);
}
