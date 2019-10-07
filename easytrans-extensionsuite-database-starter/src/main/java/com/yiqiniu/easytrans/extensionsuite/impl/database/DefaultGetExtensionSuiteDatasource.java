package com.yiqiniu.easytrans.extensionsuite.impl.database;

import javax.sql.DataSource;

import org.springframework.transaction.PlatformTransactionManager;


public class DefaultGetExtensionSuiteDatasource implements GetExtentionSuiteDatabase{
    
    public DefaultGetExtensionSuiteDatasource(DataSource dataSource, PlatformTransactionManager transManager) {
        super();
        this.dataSource = dataSource;
        this.transManager = transManager;
    }

    private DataSource dataSource;
    private PlatformTransactionManager transManager;
    
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public PlatformTransactionManager getPlatformTransactionManager() {
        return transManager;
    }

}
