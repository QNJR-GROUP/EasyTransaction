package com.yiqiniu.easytrans.protocol.aft;

import java.io.Serializable;

import com.yiqiniu.easytrans.datasource.TransStatusLogger.TransactionStatus;
import com.yiqiniu.easytrans.protocol.MethodTransactionStatus;
import com.yiqiniu.easytrans.protocol.RpcBusinessProvider;

/**
 * This will help implement the situation such as:<br>  
 * <br>
 * Write and commit a record to database to mark the execute of an RPC and then execute the RPC<br/>
 * 
 * Methods here should be idempotent
 */
public interface AfterMasterTransMethod<P extends AfterMasterTransRequest<R>, R extends Serializable> extends RpcBusinessProvider<P>{
	@MethodTransactionStatus(TransactionStatus.COMMITTED)
	R afterTransaction(P param);
}
