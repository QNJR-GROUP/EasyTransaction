package com.yiqiniu.easytrans.protocol.autocps;

import java.util.concurrent.Callable;

import com.alibaba.fescar.core.context.RootContext;

/**
 * 用于执行本地事务时，确保更新、Select for update等操作能获取正确值
 * 
 * 本方法的执行要求AbstractFescarAtMethod已被初始化
 * @author deyou
 *
 */
public class AutoCpsLocalTransactionExecutor {

    public static <R> R executeWithGlobalLockCheck(Callable<R> call) throws Exception {
        try {
            RootContext.bind("Local Tranaction with Global lock support");
            return call.call();
        } finally {
            RootContext.unbind();
        }
    }
}
