package com.yiqiniu.easytrans.monitor;

/**
 * 
 * @author deyou
 *
 */
public interface TransactionLogMonitor extends EtMonitor {
    Object getUnfinishedLogs(int pageSize,Long latestTimeStamp);
    
    Object consistentProcess(String busCode, long trxId);
    
}
