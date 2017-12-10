package com.yiqiniu.easytrans.datasource;

import javax.sql.DataSource;

import org.springframework.transaction.PlatformTransactionManager;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;

/**
 * get the using JDBC-DataSource for specific 
 *
 */
public interface DataSourceSelector {
	
	/**
	 * for the use of master transaction<br/>
	 * if it's a cascade transaction,the data source select result should be the same with arouse method's database.<br/>
	 * selectDataSource(String, String, EasyTransRequest<?, ?>) 
	 * 
	 * 主控事务发起者用于选择数据源的方法<br/>
	 * 如果这个主控事务是级联事务里的一环，那么这个数据源选择器选择出来的datasource要跟唤起本主控事务的方法所选定的数据源一致。<br/>
	 * 唤起本主控事务选定数据源的方法就是selectDataSource(String, String, EasyTransRequest<?, ?>)
	 * 
	 * @param appId
	 * @param busCode
	 * @param trxId
	 * @return
	 */
	DataSource selectDataSource(String appId,String busCode,String trxId);
	
	/**
	 * for the use of slave transaction
	 * @param slave transaction appId
	 * @param slave transaction busCode
	 * @param slave transaction request parameter
	 * @return
	 */
	DataSource selectDataSource(String appId,String busCode,EasyTransRequest<?, ?> request);
	
	/**
	 * for the use of master transaction
	 * @param appId
	 * @param busCode
	 * @param trxId
	 * @return
	 */
	PlatformTransactionManager selectTransactionManager(String appId,String busCode,String trxId);
	
	/**
	 * for the use of master transaction
	 * @param appId
	 * @param busCode
	 * @param parentTrxId
	 * @return
	 */
	PlatformTransactionManager selectTransactionManager(String appId,String busCode,EasyTransRequest<?, ?> request);
}
