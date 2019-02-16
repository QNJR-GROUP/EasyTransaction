package com.yiqiniu.easytrans.protocol.fescar;

import java.io.Serializable;

import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

/**
 * Fescar At model<br/>
 * <br/>
 */
public interface FescarAtMethod<P extends FescarAtMethodRequest<R>, R  extends Serializable> extends RpcBusinessProvider<P> {
    
	/**
	 * do business
	 * @param param
	 * @return
	 */
	@ExecuteOrder(doNotExecuteAfter = { "doFescarAtCommit", "doFescarAtRollback" }, ifNotExecutedReturnDirectly = {}, isSynchronousMethod=true)
	@MethodTransactionStatus(TransactionStatus.UNKNOWN)
	R doFescarAtBusiness(P param);
	
	/**
	 * you can append logic here when commit, usually just leave it empty
	 * @param param
	 */
	@MethodTransactionStatus(TransactionStatus.COMMITTED)
	void doFescarAtCommit(P param);
	
	/**
     * you can append logic here when roll back, usually just leave it empty
	 * @param param
	 */
	@ExecuteOrder(doNotExecuteAfter = {}, ifNotExecutedReturnDirectly = { "doFescarAtBusiness" })
	@MethodTransactionStatus(TransactionStatus.ROLLBACKED)
	void doFescarAtRollback(P param);
}
