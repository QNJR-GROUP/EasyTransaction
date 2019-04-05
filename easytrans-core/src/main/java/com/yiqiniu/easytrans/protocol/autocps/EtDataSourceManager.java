package com.yiqiniu.easytrans.protocol.autocps;

import static com.alibaba.fescar.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.DataSourceManager;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMetaCache;
import com.alibaba.fescar.rm.datasource.undo.AbstractUndoExecutor;
import com.alibaba.fescar.rm.datasource.undo.BranchUndoLog;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;
import com.alibaba.fescar.rm.datasource.undo.UndoExecutorFactory;
import com.alibaba.fescar.rm.datasource.undo.UndoLogParserFactory;
import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.filter.MetaDataFilter;

public class EtDataSourceManager extends DataSourceManager {

    private static Logger LOGGER = LoggerFactory.getLogger(EtDataSourceManager.class);
    
    public static void initEtDataSourceManager() {
        DataSourceManager.set(new EtDataSourceManager());
        LOGGER.info("Trigger EtDataSourceManager init!");
    }

    private static final String UNDO_LOG_TABLE_NAME = "undo_log";
    private static final String LOCK_TABLE_NAME = "fescar_lock";

    private static final String DELETE_UNDO_LOG_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME + "\n" + "\tWHERE branch_id = ? AND xid = ?";
    private static String SELECT_UNDO_LOG_SQL = "SELECT * FROM " + UNDO_LOG_TABLE_NAME + " WHERE log_status = 0 AND branch_id = ? AND xid = ? FOR UPDATE";

    private static final String INSERT_LOCK_SQL = "INSERT INTO " + LOCK_TABLE_NAME + "\n" + "\t(t_name, t_pk, xid, create_time)\n VALUES (?, ?, ?, now())";
    private static final String DELETE_LOCK_SQL = "DELETE FROM " + LOCK_TABLE_NAME + " WHERE t_name = ? and t_pk = ?";
    private static final String QUERY_LOCK_SQL = "SELECT COUNT(*) FROM " + LOCK_TABLE_NAME + " WHERE ";
    private static final String QUERY_LOCK_SQL_WHERE = "(t_pk in (@PK) and t_name = \"@TN\")";
    private static final String FOR_UPDATE = " FOR UPDATE";
    
    
    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String lockKey) throws TransactionException {

        Integer callSeq = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.CALL_SEQ);

        // check locks
        if (StringUtils.isNullOrEmpty(lockKey)) {
            return callSeq== null?-1:callSeq.longValue();
        }

        //ET要求使用Spring管控下的事务，因此本方法可以获得对应的当前连接,获取当前连接来执行时为了避免争夺连接词的连接导致死锁
        DataSourceProxy dataSourceProxy = get(resourceId);
        ConnectionProxy cp = (ConnectionProxy) DataSourceUtils.getConnection(dataSourceProxy);
        Connection targetConnection = cp.getTargetConnection();
        
        if (callSeq != null) {
            // callSeq != null means it's in ET transaction control
            try {
                doLockKeys(xid, lockKey, targetConnection);
            } catch (SQLException e) {
                throw new RuntimeException("Obtain Lock failed, Rollback transaction：" + lockKey, e);
            }

        } else {
            // callSeq == null means it's just a local transaction or a master transaction in ET
            // it need to check lock
            if(!lockQuery(branchType, resourceId, xid, lockKey)) {
                throw new RuntimeException("Obtain Lock failed, Rollback transaction：" + lockKey);
            }
            
            // not need to save undolog ,undo will be handle by local transaction, just hack to clean undo log
            cp.getContext().getUndoItems().clear();
        }
        
        return callSeq== null?-1:callSeq.longValue();
    }

    @Override
    public void branchReport(String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
        // do nothing here, because in ET do not have remote centralized TC
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
        
        DataSourceProxy dataSourceProxy = get(resourceId);
        //ET要求使用Spring管控下的事务，因此本方法可以获得对应的当前连接,获取当前连接来执行时为了避免争夺连接词的连接导致死锁
        ConnectionProxy cp = (ConnectionProxy) DataSourceUtils.getConnection(dataSourceProxy);
        Connection targetConnection = cp.getTargetConnection();
        try {
            return queryLockKeys(xid, lockKeys, targetConnection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerResource(Resource resource) {
        DataSourceProxy dataSourceProxy = (DataSourceProxy) resource;
        getManagedResources().put(dataSourceProxy.getResourceId(), dataSourceProxy);
    }

    @Override
    public BranchStatus branchCommit(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {

        executeInTransaction(resourceId, new ExecuteWithConn<Void>() {

            @Override
            public Void execute(Connection conn) throws SQLException {
                
                //get undo log
                List<String> rollbackInfos = getRollbackInfo(xid, branchId, conn, resourceId);
                
                // clean the lock
                cleanLocks(xid, branchId, conn, rollbackInfos, resourceId);

                // clean the log
                deleteUndoLog(xid, branchId, conn);
                
                return null;
            }
        });

        return BranchStatus.PhaseTwo_Committed;
    }

    @Override
    public BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {

        executeInTransaction(resourceId, new ExecuteWithConn<Void>() {

            @Override
            public Void execute(Connection conn) throws SQLException {

                // rollback info
                List<String> rollbackInfos = getRollbackInfo(xid, branchId, conn, resourceId);

                // recover the records
                undoRecords(conn, resourceId, rollbackInfos);

                // clean the lock
                cleanLocks(xid, branchId, conn, rollbackInfos, resourceId);

                // clean the log
                deleteUndoLog(xid, branchId, conn);
                
                return null;
            }
        });
        return BranchStatus.PhaseTwo_Rollbacked;
    }

    private void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException {
        PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_SQL);
        deletePST.setLong(1, branchId);
        deletePST.setString(2, xid);
        deletePST.executeUpdate();
    }

    private void undoRecords(Connection conn, String resourceId, List<String> rollbackInfos) throws SQLException {

        DataSourceProxy dataSourceProxy = get(resourceId);

        for (String rollbackInfo : rollbackInfos) {
            BranchUndoLog branchUndoLog = UndoLogParserFactory.getInstance().decode(rollbackInfo);

            for (SQLUndoLog sqlUndoLog : branchUndoLog.getSqlUndoLogs()) {
                TableMeta tableMeta = TableMetaCache.getTableMeta(dataSourceProxy, sqlUndoLog.getTableName());
                sqlUndoLog.setTableMeta(tableMeta);
                AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(dataSourceProxy.getDbType(), sqlUndoLog);
                undoExecutor.executeOn(conn);
            }

        }

    }

    private List<String> getRollbackInfo(String xid, long branchId, Connection conn, String resourceId) throws SQLException {

        ResultSet rs = null;
        PreparedStatement selectPST = null;
        selectPST = conn.prepareStatement(SELECT_UNDO_LOG_SQL);
        selectPST.setLong(1, branchId);
        selectPST.setString(2, xid);
        rs = selectPST.executeQuery();

        List<String> result = new ArrayList<>();

        try {
            while (rs.next()) {
                Blob b = rs.getBlob("rollback_info");
                String rollbackInfo = StringUtils.blob2string(b);
                result.add(rollbackInfo);
            }
        } finally {
            rs.close();
        }

        return result;

    }

    private void cleanLocks(String xid, long branchId, Connection conn, List<String> rollbackInfos, String resourceId) throws SQLException {

        DataSourceProxy dataSourceProxy = get(resourceId);
        PreparedStatement pdst = conn.prepareStatement(DELETE_LOCK_SQL);

        int cleanRecords = 0;
        for (String rollbackInfo : rollbackInfos) {
            BranchUndoLog branchUndoLog = UndoLogParserFactory.getInstance().decode(rollbackInfo);

            for (SQLUndoLog sqlUndoLog : branchUndoLog.getSqlUndoLogs()) {

                Set<String> tableLockKeys = new HashSet<String>();

                String tableName = sqlUndoLog.getTableName().replace("`", "");
                TableMeta tableMeta = TableMetaCache.getTableMeta(dataSourceProxy, tableName);
                sqlUndoLog.setTableMeta(tableMeta);

                // TODO it assumes only support single filed primary key
                List<Field> beforeImage = sqlUndoLog.getBeforeImage().pkRows();
                List<Field> afterImage = sqlUndoLog.getAfterImage().pkRows();

                tableLockKeys.addAll(beforeImage.stream().map(k -> k.getValue().toString()).collect(Collectors.toList()));
                tableLockKeys.addAll(afterImage.stream().map(k -> k.getValue().toString()).collect(Collectors.toList()));

                for (String key : tableLockKeys) {
                    pdst.setString(1, tableName);
                    pdst.setString(2, key);
                    pdst.addBatch();
                    cleanRecords++;
                }
            }
        }
        
        if (cleanRecords > 0) {
            pdst.executeUpdate();
        }
    }

    private void doLockKeys(String xid, String lockKey, Connection conn) throws SQLException {
        int count = 0;
        PreparedStatement insertPST = conn.prepareStatement(INSERT_LOCK_SQL);
        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                throw new ShouldNeverHappenException("Wrong format of LOCK KEYS: " + lockKey);
            }

            String tableName = tableGroupedLockKey.substring(0, idx).replace("`", "");
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);

            String[] pks = mergedPKs.split(",");

            // remove duplicated keys
            HashSet<String> setPks = new HashSet<>(pks.length);
            setPks.addAll(Arrays.asList(pks));

            for (String pk : pks) {
                insertPST.setString(1, tableName);
                insertPST.setString(2, pk);
                insertPST.setString(3, xid);
                insertPST.addBatch();
                count++;
            }
        }

        int executeUpdate = insertPST.executeUpdate();
        if (count != executeUpdate) {
            throw new RuntimeException("update count not match! updateCount: " + executeUpdate + " suppose to be :" + count + " keys: " + lockKey);
        }
    }
    
    private boolean queryLockKeys(String xid, String lockKey, Connection conn) throws SQLException {
        
        StringBuilder fullSql = new StringBuilder(QUERY_LOCK_SQL);
        
        String[] tableGroupedLockKeys = lockKey.split(";");
        for (int pos = 0; pos < tableGroupedLockKeys.length; pos++) {
            String tableGroupedLockKey = tableGroupedLockKeys[pos];
            
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                throw new ShouldNeverHappenException("Wrong format of LOCK KEYS: " + lockKey);
            }

            String tableName = tableGroupedLockKey.substring(0, idx).replace("`", "");;
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);

            String[] pks = mergedPKs.split(",");

            // remove duplicated keys
            HashSet<String> setPks = new HashSet<>(pks.length);
            setPks.addAll(Arrays.asList(pks));
            String tableStatment = QUERY_LOCK_SQL_WHERE.replaceFirst("@TN", tableName);
            
            // generate PK 
            StringBuilder sbPks = new StringBuilder();
            for (int i = 0; i < pks.length; i++) {
                sbPks.append("\"");
                sbPks.append(pks[i]);
                sbPks.append("\"");
                if(pks.length < i-1) {
                    sbPks.append(",");
                }
            }
            
            fullSql.append(tableStatment.replaceFirst("@PK", sbPks.toString()));
            if(pos < tableGroupedLockKeys.length - 1) {
                fullSql.append(" or ");
            }
        }
        
        fullSql.append(FOR_UPDATE);
        
        try(ResultSet rs = conn.createStatement().executeQuery(fullSql.toString())) {
            while (rs.next()) {
                int selected = rs.getInt(1);
                if(selected > 0) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private static interface ExecuteWithConn<R> {
        R execute(Connection conn) throws SQLException;
    }

    private <R> R executeInTransaction(String resourceId, ExecuteWithConn<R> execute) throws TransactionException {
        
        
        DataSourceProxy dataSourceProxy = get(resourceId);

        Connection conn = null;
        PreparedStatement selectPST = null;
        try {
            
            conn = dataSourceProxy.getPlainConnection();

            // The entire undo process should run in a local transaction.
            conn.setAutoCommit(false);

            R r = execute.execute(conn);

            conn.commit();
            
            return r;

        } catch (Throwable e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.warn("Failed to close JDBC resource ... ", rollbackEx);
                }
            }
            throw new TransactionException(BranchRollbackFailed_Retriable, e);

        } finally {
            try {
                if (selectPST != null) {
                    selectPST.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException closeEx) {
                LOGGER.warn("Failed to close JDBC resource ... ", closeEx);
            }
        }
    }


}
