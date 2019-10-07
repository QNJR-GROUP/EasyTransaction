package com.yiqiniu.easytrans.protocol.autocps;

import java.io.Serializable;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.protocol.TransactionId;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.DefaultResourceManager;

public abstract class AbstractAutoCpsMethod<P extends AutoCpsMethodRequest<R>, R extends Serializable> implements AutoCpsMethod<P, R> {
    
    {
        EtDataSourceManager.initEtDataSourceManager();
    }

    private static Logger LOGGER = LoggerFactory.getLogger(AbstractAutoCpsMethod.class);

    @Override
    public final R doAutoCpsBusiness(P param) {

        TransactionId transactionId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
        String xid = getFescarXid(transactionId);

        try {
            RootContext.bind(xid);
            return doBusiness(param);
        } finally {
            RootContext.unbind();
        }
    }

    protected abstract R doBusiness(P param);

    @Override
    public final void doAutoCpsCommit(P param) {

        Integer callSeq = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.CALL_SEQ);
        TransactionId transactionId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
        DataSource ds = MetaDataFilter.getMetaData(EasytransConstant.DataSourceRelative.DATA_SOURCE);

        if (ds instanceof Resource) {
            Resource rs = (Resource) ds;
            try {
                DefaultResourceManager.get().branchCommit(BranchType.AT,getFescarXid(transactionId), callSeq, rs.getResourceId(), null);
            } catch (TransactionException e) {
                LOGGER.error("transaction commit exception occour , code:" + e.getCode(), e);
                throw new RuntimeException("transaction exception", e);
            }
        } else {
            throw new RuntimeException(ds.toString() + " is not DataSourceProxy, please correct the config!");
        }

    }

    private String getFescarXid(TransactionId transactionId) {
        return transactionId.getAppId() + "|" + transactionId.getBusCode() + "|" + transactionId.getTrxId();
    }

    @Override
    public final void doAutoCpsRollback(P param) {

        Integer callSeq = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.CALL_SEQ);
        TransactionId transactionId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
        DataSource ds = MetaDataFilter.getMetaData(EasytransConstant.DataSourceRelative.DATA_SOURCE);

        if (ds instanceof Resource) {
            Resource rs = (Resource) ds;
            try {
                DefaultResourceManager.get().branchRollback(BranchType.AT,getFescarXid(transactionId), callSeq, rs.getResourceId(), null);
            } catch (TransactionException e) {
                LOGGER.error("transaction roll back exception occour , code:" + e.getCode(), e);
                throw new RuntimeException("transaction exception", e);
            }
        } else {
            throw new RuntimeException(ds.toString() + " is not DataSourceProxy, please correct the config!");
        }

    }

}
