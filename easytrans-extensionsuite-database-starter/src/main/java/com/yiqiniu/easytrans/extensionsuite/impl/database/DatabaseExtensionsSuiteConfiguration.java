package com.yiqiniu.easytrans.extensionsuite.impl.database;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.alibaba.druid.pool.DruidDataSource;
import com.yiqiniu.easytrans.idgen.TrxIdGenerator;
import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.impl.database.DataBaseTransactionLogCleanJob;
import com.yiqiniu.easytrans.log.impl.database.DataBaseTransactionLogReaderImpl;
import com.yiqiniu.easytrans.log.impl.database.DataBaseTransactionLogWritterImpl;
import com.yiqiniu.easytrans.master.EasyTransMasterSelector;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.stringcodec.StringCodec;
import com.yiqiniu.easytrans.util.ByteFormIdCodec;

/** 
* @author xudeyou 
*/
@Configuration
@ConditionalOnProperty(name="easytrans.extensionsuite.database.enabled",havingValue="true",matchIfMissing=true)
@EnableConfigurationProperties(DatabaseExtensionSuiteProperties.class)
public class DatabaseExtensionsSuiteConfiguration {
	
	@Value("${spring.application.name}")
	private String applicationName;
	
	@Bean
	@ConditionalOnMissingBean(GetExtentionSuiteDatabase.class)
	public GetExtentionSuiteDatabase getMasterSelectorDatasource(DatabaseExtensionSuiteProperties properties) {
	    
        DruidDataSource datasource = new DruidDataSource();
        Map<String, String> druidPropertyMap = properties.getDbSetting();
        Properties druidProperties = new Properties();
        for (Entry<String, String> entry : druidPropertyMap.entrySet()) {
            druidProperties.put("druid." + entry.getKey(), entry.getValue());
        }
        datasource.configFromPropety(druidProperties);
	    
	    return new DefaultGetExtensionSuiteDatasource(datasource, new DataSourceTransactionManager(datasource));
	}
	
	@Bean
	@ConditionalOnMissingBean(EasyTransMasterSelector.class)
	public DatabaseMasterSelectorImpl databaseMasterSelectorImpl(DatabaseExtensionSuiteProperties properties,GetExtentionSuiteDatabase dataSourceGetter){
		return new DatabaseMasterSelectorImpl(properties.getTablePrefix(), dataSourceGetter.getDataSource(), dataSourceGetter.getPlatformTransactionManager(), applicationName,60);
	}
	
	@Bean
	@ConditionalOnMissingBean(TrxIdGenerator.class)
	public TrxIdGenerator trxIdGenerator(DatabaseMasterSelectorImpl dbMasterSelector){
	    //TODO instanceId may change during processing,but it will not change in Id generator
	    return new DatabaseSnowFlakeIdGenerator(dbMasterSelector.getInstanceId());
	}
	
	@Bean
	@ConditionalOnMissingBean(StringCodec.class)
	public StringCodec stringCodec(DatabaseExtensionSuiteProperties properties,GetExtentionSuiteDatabase dataSourceGetter){
	    return new DatabaseStringCodecImpl(properties.getTablePrefix(), dataSourceGetter.getDataSource(), dataSourceGetter.getPlatformTransactionManager());
	}
	
	
	// configuration below is for log-database

    @Bean
    @ConditionalOnProperty(name = { "logCleanEnabled" }, prefix = "easytrans.log.database")
    public DataBaseTransactionLogCleanJob logCleanJob(EasyTransMasterSelector master, DataBaseTransactionLogWritterImpl logWritter, DatabaseExtensionSuiteProperties properties) {
        return new DataBaseTransactionLogCleanJob(applicationName, master, logWritter, properties.getLogReservedDays(), properties.getLogCleanTime());
    }

    @Bean
    @ConditionalOnMissingBean(TransactionLogReader.class)
    public DataBaseTransactionLogReaderImpl dataBaseTransactionLogReaderImpl(ObjectSerializer serializer, GetExtentionSuiteDatabase dataBaseWrap, ByteFormIdCodec idCodec, DatabaseExtensionSuiteProperties properties) {
        return new DataBaseTransactionLogReaderImpl(applicationName, serializer, dataBaseWrap.getDataSource(), idCodec, properties.getTablePrefix());
    }

    @Bean
    @ConditionalOnMissingBean(TransactionLogWritter.class)
    public DataBaseTransactionLogWritterImpl dataBaseTransactionLogWritterImpl(ObjectSerializer serializer, GetExtentionSuiteDatabase dataBaseWrap, ByteFormIdCodec idCodec, DatabaseExtensionSuiteProperties properties) {
        return new DataBaseTransactionLogWritterImpl(serializer, dataBaseWrap.getDataSource(), idCodec, properties.getTablePrefix());
    }
}
