package com.yiqiniu.easytrans.protocol.autocps;

import java.io.Serializable;

import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

/**
 * Fescar At model<br/>
 * <br/>
 */
public interface AutoCpsMethod<P extends AutoCpsMethodRequest<R>, R  extends Serializable> extends RpcBusinessProvider<P> {
    
	public static final String DO_AUTO_CPS_BUSINESS = "doAutoCpsBusiness";
    public static final String DO_AUTO_CPS_ROLLBACK = "doAutoCpsRollback";
    public static final String DO_AUTO_CPS_COMMIT = "doAutoCpsCommit";

    /**
	 * do business
	 * @param param
	 * @return
	 */
	@ExecuteOrder(doNotExecuteAfter = { DO_AUTO_CPS_COMMIT, DO_AUTO_CPS_ROLLBACK }, ifNotExecutedReturnDirectly = {}, isSynchronousMethod=true)
	@MethodTransactionStatus(TransactionStatus.UNKNOWN)
	R doAutoCpsBusiness(P param);
	
	/**
	 * you can append logic here when commit, usually just leave it empty
	 * @param param
	 */
	@MethodTransactionStatus(TransactionStatus.COMMITTED)
	void doAutoCpsCommit(P param);
	
	/**
     * you can append logic here when roll back, usually just leave it empty
	 * @param param
	 */
	@ExecuteOrder(doNotExecuteAfter = {}, ifNotExecutedReturnDirectly = { DO_AUTO_CPS_BUSINESS })
	@MethodTransactionStatus(TransactionStatus.ROLLBACKED)
	void doAutoCpsRollback(P param);
}
