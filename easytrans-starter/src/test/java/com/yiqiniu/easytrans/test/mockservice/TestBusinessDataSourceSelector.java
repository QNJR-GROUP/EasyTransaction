package com.yiqiniu.easytrans.test.mockservice;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

@Component
public class TestBusinessDataSourceSelector implements DataSourceSelector {

	@Resource
	private ApplicationContext ctx;
	
	@Override
	public DataSource selectDataSource(String appId, String busCode, String trxId) {
		if(appId != null){
			//无论是否递归的业务都是同一个数据源
			busCode = busCode.replace("Cascade", "");
			return ctx.getBean(busCode, DataSource.class);
		}else{
			return ctx.getBean("whole", DataSource.class);
		}
	}

	@Override
	public DataSource selectDataSource(String appId, String busCode,
			EasyTransRequest<?, ?> request) {
		if(appId != null){
			busCode = busCode.replace("Cascade", "");
			return ctx.getBean(busCode, DataSource.class);
		}else{
			return ctx.getBean("whole", DataSource.class);
		}
	}

	@Override
	public PlatformTransactionManager selectTransactionManager(String appId, String busCode, String trxId) {
		if(appId != null){
			busCode = busCode.replace("Cascade", "");
			return ctx.getBean(busCode+"TransactionManager", PlatformTransactionManager.class);
		}else{
			return ctx.getBean("wholeTransactionManager", PlatformTransactionManager.class);
		}
	}

	@Override
	public PlatformTransactionManager selectTransactionManager(String appId, String busCode, EasyTransRequest<?, ?> request) {
		if(appId != null){
			busCode = busCode.replace("Cascade", "");
			return ctx.getBean(busCode+"TransactionManager", PlatformTransactionManager.class);
		}else{
			return ctx.getBean("wholeTransactionManager", PlatformTransactionManager.class);
		}
	}

}
