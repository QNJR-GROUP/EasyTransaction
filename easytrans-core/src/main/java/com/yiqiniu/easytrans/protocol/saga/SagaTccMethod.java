package com.yiqiniu.easytrans.protocol.saga;

import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

/**
 * SAGA FORM TCC<br/>
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
public interface SagaTccMethod<P extends SagaTccMethodRequest> extends RpcBusinessProvider<P> {
    
	/**
	 * reserve the resources that will be used in doConifirm(),so doConfirm() is always practicable in business
	 * @param param
	 * @return
	 */
	@ExecuteOrder(doNotExecuteAfter = { "sagaConfirm", "sagaCancel" }, ifNotExecutedReturnDirectly = {}, isSynchronousMethod=true)
	@MethodTransactionStatus(TransactionStatus.UNKNOWN)
	void sagaTry(P param);
	
	/**
	 * consume the resources reserved in doTry and finish the transaction
	 * @param param
	 */
	@MethodTransactionStatus(TransactionStatus.COMMITTED)
	void sagaConfirm(P param);
	
	/**
	 * release the resources reserved in doTry so the resources can be use by other transactions
	 * @param param
	 */
	@ExecuteOrder(doNotExecuteAfter = {}, ifNotExecutedReturnDirectly = { "sagaTry" })
	@MethodTransactionStatus(TransactionStatus.ROLLBACKED)
	void sagaCancel(P param);
}
