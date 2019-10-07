package com.yiqiniu.easytrans.extensionsuite.impl.database;

import javax.sql.DataSource;

import org.springframework.transaction.PlatformTransactionManager;

public interface GetExtentionSuiteDatabase {

    DataSource getDataSource();
    
    PlatformTransactionManager getPlatformTransactionManager();
    
}
