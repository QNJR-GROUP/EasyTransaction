package com.yiqiniu.easytrans.idempotent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class IdempotentHelper {
	
	private DataSourceSelector selector;
	private ListableProviderFactory providerFactory;
	
	public IdempotentHelper(DataSourceSelector selector, ListableProviderFactory providerFactory) {
		super();
		this.selector = selector;
		this.providerFactory = providerFactory;
	}

	private static final String TRANSACTION_MANAGER = "TRANSACTION_MANAGER";
	private static final String DATA_SOURCE = "DATA_SOURCE";
	private static final String JDBC_TEMPLATE = "JDBC_TEMPLATE";
	
	public PlatformTransactionManager getTransactionManager(EasyTransFilterChain filterChain, EasyTransRequest<?, ?> reqest){
		PlatformTransactionManager transactionManager = filterChain.getResource(TRANSACTION_MANAGER);
		if(transactionManager == null){
			transactionManager = selector.selectTransactionManager(filterChain.getAppId(), filterChain.getBusCode(),reqest);
			filterChain.bindResource(TRANSACTION_MANAGER, transactionManager);
		}
		
		return transactionManager;
	}
	
	public int getIdempotentType(EasyTransRequest<?, ?> request){
		BusinessIdentifer businessIdentifer = request.getClass().getAnnotation(BusinessIdentifer.class);
		BusinessProvider<?> rpcBusinessProvider = (BusinessProvider<?>) providerFactory.getService(businessIdentifer.appId(), businessIdentifer.busCode());
		return rpcBusinessProvider.getIdempotentType();
	}
	
	public DataSource getDatasource(EasyTransFilterChain filterChain, EasyTransRequest<?, ?> reqest){
		DataSource dataSource = filterChain.getResource(DATA_SOURCE);
		if(dataSource == null){
			dataSource = selector.selectDataSource(filterChain.getAppId(), filterChain.getBusCode(),reqest);
			filterChain.bindResource(DATA_SOURCE, dataSource);
		}
		return dataSource;
	}
	
	public JdbcTemplate getJdbcTemplate(EasyTransFilterChain filterChain, EasyTransRequest<?, ?> reqest){
		JdbcTemplate jdbcTemplate = filterChain.getResource(JDBC_TEMPLATE);
		if(jdbcTemplate == null){
			DataSource datasource = getDatasource(filterChain, reqest);
			jdbcTemplate = new JdbcTemplate(datasource);
			filterChain.bindResource(JDBC_TEMPLATE, jdbcTemplate);
		}
		return jdbcTemplate;
	}

	private BeanPropertyRowMapper<IdempotentPo> beanPropertyRowMapper = new BeanPropertyRowMapper<IdempotentPo>(IdempotentPo.class);
	/**
	 * get execute result from database
	 * @param filterChain
	 * @param reqest
	 * @return
	 */
	public IdempotentPo getIdempotentPo(EasyTransFilterChain filterChain, Map<String,Object> header, EasyTransRequest<?, ?> reqest){
		BusinessIdentifer businessType = ReflectUtil.getBusinessIdentifer(reqest.getClass());
		Object trxIdObj = header.get(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
		TransactionId transactionId = (TransactionId) trxIdObj;
		Integer callSeq = Integer.parseInt(header.get(EasytransConstant.CallHeadKeys.CALL_SEQ).toString());
		JdbcTemplate jdbcTemplate = getJdbcTemplate(filterChain, reqest);
		 List<IdempotentPo> listQuery = jdbcTemplate.query(
				"select * from idempotent where src_app_id = ? and src_bus_code = ? and src_trx_id = ? and app_id = ? and bus_code = ? and call_seq = ?", 
				new Object[]{
						transactionId.getAppId(),
						transactionId.getBusCode(),
						transactionId.getTrxId(),
						businessType.appId(),
						businessType.busCode(),
						callSeq},
				beanPropertyRowMapper
				);
		 
		 if(listQuery.size() == 1){
			 return listQuery.get(0);
		 }else if (listQuery.size() == 0){
			 return null;
		 }else{
			 throw new RuntimeException("Unkonw Error!" + listQuery);
		 }
	}

	
	private ConcurrentHashMap<String, Object> mapExecuteOrder = new ConcurrentHashMap<String, Object>();
	private static final Object NULL_OBJECT = new Object();
	public ExecuteOrder getExecuteOrder(String appId, String busCode ,String innerMethod) {
		
//		if(EasyTransFilterChain.MESSAGE_BUSINESS_FLAG.equals(innerMethod)){
//			return null;
//		}
		
		String key = getKey(appId, busCode, innerMethod);
		Object object = mapExecuteOrder.get(key);
		if(object == null){
			Class<?> serviceInterface = providerFactory.getServiceInterface(appId, busCode);
			Method[] methods = serviceInterface.getMethods();
			for(Method method:methods){
				if(method.getName().equals(innerMethod)){
					ExecuteOrder annotation = method.getAnnotation(ExecuteOrder.class);
					if(annotation == null){
						object = NULL_OBJECT;
					}else{
						object = annotation;
					}
					mapExecuteOrder.put(key, object);
					break;
				}
			}
		}
		
		if(object == NULL_OBJECT){
			return null;
		}else{
			return (ExecuteOrder) object;
		}
	}

	private String getKey(String appId, String busCode ,String innerMethod){
		return appId + busCode + innerMethod;
	}

	public static class IdempotentPo{
		private String srcAppId;
		private String srcBusCode;
		private String srcTrxId;
		private String appId;
		private String busCode;
		private Integer callSeq;
		private String calledMethods;
		private String md5;
		private byte[] syncMethodResult;
		private Date createTime;
		private Date updateTime;
		private Integer lockVersion;
		public String getSrcAppId() {
			return srcAppId;
		}
		public void setSrcAppId(String srcAppId) {
			this.srcAppId = srcAppId;
		}
		public String getSrcBusCode() {
			return srcBusCode;
		}
		public void setSrcBusCode(String srcBusCode) {
			this.srcBusCode = srcBusCode;
		}
		public String getSrcTrxId() {
			return srcTrxId;
		}
		public void setSrcTrxId(String srcTrxId) {
			this.srcTrxId = srcTrxId;
		}
		public String getAppId() {
			return appId;
		}
		public void setAppId(String appId) {
			this.appId = appId;
		}
		public String getBusCode() {
			return busCode;
		}
		public void setBusCode(String busCode) {
			this.busCode = busCode;
		}
		
		public Integer getCallSeq() {
			return callSeq;
		}
		public void setCallSeq(Integer callSeq) {
			this.callSeq = callSeq;
		}
		public String getCalledMethods() {
			return calledMethods;
		}
		public void setCalledMethods(String calledMethods) {
			this.calledMethods = calledMethods;
		}
		public String getMd5() {
			return md5;
		}
		public void setMd5(String md5) {
			this.md5 = md5;
		}
		public byte[] getSyncMethodResult() {
			return syncMethodResult;
		}
		public void setSyncMethodResult(byte[] syncMethodResult) {
			this.syncMethodResult = syncMethodResult;
		}
		public Date getCreateTime() {
			return createTime;
		}
		public void setCreateTime(Date createTime) {
			this.createTime = createTime;
		}
		public Date getUpdateTime() {
			return updateTime;
		}
		public void setUpdateTime(Date updateTime) {
			this.updateTime = updateTime;
		}
		public Integer getLockVersion() {
			return lockVersion;
		}
		public void setLockVersion(Integer lockVersion) {
			this.lockVersion = lockVersion;
		}
		@Override
		public String toString() {
			return "IdempotentPo [srcAppId=" + srcAppId + ", srcBusCode="
					+ srcBusCode + ", srcTrxId=" + srcTrxId + ", appId="
					+ appId + ", busCode=" + busCode + ", calledMethods="
					+ calledMethods + ", md5=" + md5 + ", syncMethodResult="
					+ Arrays.toString(syncMethodResult) + ", createTime="
					+ createTime + ", updateTime=" + updateTime
					+ ", lockVersion=" + lockVersion + "]";
		}
	}

	public void saveIdempotentPo(EasyTransFilterChain filterChain, IdempotentPo idempotentPo) {
		JdbcTemplate jdbcTemplate = filterChain.getResource(JDBC_TEMPLATE);
		int update = jdbcTemplate.update(
				"INSERT INTO `idempotent` (`src_app_id`, `src_bus_code`, `src_trx_id`, `app_id`, `bus_code`, `call_seq` ,`called_methods`, `md5`, `sync_method_result`, `create_time`, `update_time` , `lock_version`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", 
				idempotentPo.getSrcAppId(),
				idempotentPo.getSrcBusCode(),
				idempotentPo.getSrcTrxId(),
				idempotentPo.getAppId(),
				idempotentPo.getBusCode(),
				idempotentPo.getCallSeq(),
				idempotentPo.getCalledMethods(),
				idempotentPo.getMd5(),
				idempotentPo.getSyncMethodResult(),
				idempotentPo.getCreateTime(),
				idempotentPo.getUpdateTime(),
				idempotentPo.getLockVersion()
				);
		
		if(update != 1){
			throw new RuntimeException("update count exception!" + update);
		}
	}
	
	public void updateIdempotentPo(EasyTransFilterChain filterChain, IdempotentPo idempotentPo) {

		JdbcTemplate jdbcTemplate = filterChain.getResource(JDBC_TEMPLATE);
		int update = jdbcTemplate.update(
				"UPDATE `idempotent` SET `called_methods` = ?, `md5` = ?, `sync_method_result` = ?, `create_time` = ?, `update_time`  = ?, `lock_version` = `lock_version` + 1 WHERE `src_app_id` = ? AND `src_bus_code` = ? AND `src_trx_id` = ? AND `app_id` = ? AND `bus_code` = ?  AND `call_seq` = ? AND `lock_version` = ?;", 
				idempotentPo.getCalledMethods(),
				idempotentPo.getMd5(),
				idempotentPo.getSyncMethodResult(),
				idempotentPo.getCreateTime(),
				idempotentPo.getUpdateTime(),
				idempotentPo.getSrcAppId(),
				idempotentPo.getSrcBusCode(),
				idempotentPo.getSrcTrxId(),
				idempotentPo.getAppId(),
				idempotentPo.getBusCode(),
				idempotentPo.getCallSeq(),
				idempotentPo.getLockVersion()
				);
		
		if(update != 1){
			throw new RuntimeException("Optimistic Lock Error Occour Or can not find the specific Record!" + idempotentPo);
		}
	}
}
