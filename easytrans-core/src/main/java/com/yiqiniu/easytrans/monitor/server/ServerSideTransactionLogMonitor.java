package com.yiqiniu.easytrans.monitor.server;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.yiqiniu.easytrans.core.ConsistentGuardian;
import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.monitor.TransactionLogMonitor;
import com.yiqiniu.easytrans.protocol.TransactionId;

public class ServerSideTransactionLogMonitor implements TransactionLogMonitor {
    
    private String appId;
    private TransactionLogReader logReader;
    private ConsistentGuardian consistentGuardian;
    
    
    public ServerSideTransactionLogMonitor(String appId, TransactionLogReader logReader, ConsistentGuardian consistentGuardian) {
        super();
        this.appId = appId;
        this.logReader = logReader;
        this.consistentGuardian = consistentGuardian;
    }

    @Override
    public Object getUnfinishedLogs(int pageSize, Long latestTimeStamp) {
        return logReader.getUnfinishedLogs(null, pageSize, latestTimeStamp != null?new Date(latestTimeStamp):new Date());
    }

    @Override
    public Object consistentProcess(String busCode, long trxId) {
        List<LogCollection> transactionLogById = logReader.getTransactionLogById(Arrays.asList(new TransactionId(appId,busCode,trxId)));
        if(CollectionUtils.isEmpty(transactionLogById)) {
            return false;
        }
        
        consistentGuardian.process(transactionLogById.get(0));
        return true;
    }

}
