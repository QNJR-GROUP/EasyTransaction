package com.yiqiniu.easytrans.protocol.fescar;

import java.io.Serializable;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.rm.datasource.DataSourceManager;
import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.protocol.TransactionId;

public abstract class AbstractFescarAtMethod<P extends FescarAtMethodRequest<R>, R extends Serializable> implements FescarAtMethod<P, R> {
    
    {
        EtDataSourceManager.initEtDataSourceManager();
    }

    private static Logger LOGGER = LoggerFactory.getLogger(AbstractFescarAtMethod.class);

    @Override
    public final R doFescarAtBusiness(P param) {

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
    public final void doFescarAtCommit(P param) {

        Integer callSeq = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.CALL_SEQ);
        TransactionId transactionId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
        DataSource ds = MetaDataFilter.getMetaData(EasytransConstant.DataSourceRelative.DATA_SOURCE);

        if (ds instanceof Resource) {
            Resource rs = (Resource) ds;
            try {
                DataSourceManager.get().branchCommit(getFescarXid(transactionId), callSeq, rs.getResourceId(), null);
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
    public final void doFescarAtRollback(P param) {

        Integer callSeq = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.CALL_SEQ);
        TransactionId transactionId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
        DataSource ds = MetaDataFilter.getMetaData(EasytransConstant.DataSourceRelative.DATA_SOURCE);

        if (ds instanceof Resource) {
            Resource rs = (Resource) ds;
            try {
                DataSourceManager.get().branchRollback(getFescarXid(transactionId), callSeq, rs.getResourceId(), null);
            } catch (TransactionException e) {
                LOGGER.error("transaction roll back exception occour , code:" + e.getCode(), e);
                throw new RuntimeException("transaction exception", e);
            }
        } else {
            throw new RuntimeException(ds.toString() + " is not DataSourceProxy, please correct the config!");
        }

    }

}
