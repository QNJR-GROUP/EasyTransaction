package com.yiqiniu.easytrans.idempotent;

import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.APP_ID;
import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.BUSINESS_CODE;
import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.METHOD_NAME;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.datasource.DataSourceSelector;
import com.yiqiniu.easytrans.filter.EasyTransFilterChain;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.ExecuteOrder;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.provider.factory.ListableProviderFactory;
import com.yiqiniu.easytrans.stringcodec.StringCodec;
import com.yiqiniu.easytrans.util.ObjectDigestUtil;
import com.yiqiniu.easytrans.util.ReflectUtil;

public class IdempotentHelper {
	
	private String updateSql = "UPDATE `idempotent` SET `called_methods` = ?, `md5` = ?, `sync_method_result` = ?, `create_time` = ?, `update_time`  = ?, `lock_version` = `lock_version` + 1 WHERE `src_app_id` = ? AND `src_bus_code` = ? AND `src_trx_id` = ? AND `app_id` = ? AND `bus_code` = ?  AND `call_seq` = ? AND `handler` = ? AND `lock_version` = ?;";
	private String insertSql = "INSERT INTO `idempotent` (`src_app_id`, `src_bus_code`, `src_trx_id`, `app_id`, `bus_code`, `call_seq` , `handler` ,`called_methods`, `md5`, `sync_method_result`, `create_time`, `update_time` , `lock_version`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private String selectSql = "select * from idempotent where src_app_id = ? and src_bus_code = ? and src_trx_id = ? and app_id = ? and bus_code = ? and call_seq = ? and handler = ?";
	
	private DataSourceSelector selector;
	private ListableProviderFactory providerFactory;
	private String appId;
	private StringCodec stringCodecer;
	
	public IdempotentHelper(String appId, DataSourceSelector selector, ListableProviderFactory providerFactory, StringCodec stringCodecer, String tablePrefix) {
		super();
		this.selector = selector;
		this.providerFactory = providerFactory;
		this.appId = appId;
		this.stringCodecer = stringCodecer;
		
		if(!StringUtils.isEmpty(tablePrefix)) {
			tablePrefix = tablePrefix.trim();
			updateSql = updateSql.replace("idempotent", tablePrefix + "idempotent");
			insertSql = insertSql.replace("idempotent", tablePrefix + "idempotent");
			selectSql = selectSql.replace("idempotent", tablePrefix + "idempotent");
		}
		
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
	

	private BeanPropertyRowMapper<IdempotentPo> beanPropertyRowMapper = new BeanPropertyRowMapper<IdempotentPo>(IdempotentPo.class) {
		
		private String transform(ResultSet rs,int index, String strType) throws SQLException {
			Integer intermediateObj = (Integer) JdbcUtils.getResultSetValue(rs, index, Integer.class);
			return stringCodecer.findString(strType, intermediateObj);
		}
		
		@Override
		protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws java.sql.SQLException {

			ResultSetMetaData metaData = rs.getMetaData();
			String columnName = metaData.getColumnName(index).toLowerCase();
			switch(columnName) {
				case "src_app_id":
				case "app_id":
				case "handler":
					return transform(rs, index, APP_ID);
				case "src_bus_code":
				case "bus_code":
					return transform(rs, index, BUSINESS_CODE);
				case "called_methods":
					return id2callMethodsString((String) JdbcUtils.getResultSetValue(rs, index, String.class));
				case "md5":
					byte[] md5Bytes = (byte[]) JdbcUtils.getResultSetValue(rs, index, byte[].class);
					return ObjectDigestUtil.byteArrayToHexString(md5Bytes);
				default:
					return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
			}
		};
	};
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
				selectSql, 
				new Object[]{
						stringCodecer.findId(APP_ID, transactionId.getAppId()),
						stringCodecer.findId(BUSINESS_CODE,transactionId.getBusCode()),
						transactionId.getTrxId(),
						stringCodecer.findId(APP_ID, businessType.appId()),
						stringCodecer.findId(BUSINESS_CODE,businessType.busCode()),
						callSeq,
						stringCodecer.findId(APP_ID,appId)},
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
		private Long srcTrxId;
		private String appId;
		private String busCode;
		private Integer callSeq;
		private String handler;
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
		public Long getSrcTrxId() {
			return srcTrxId;
		}
		public void setSrcTrxId(Long srcTrxId) {
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
		
		public String getHandler() {
			return handler;
		}
		public void setHandler(String handler) {
			this.handler = handler;
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
			return "IdempotentPo [srcAppId=" + srcAppId + ", srcBusCode=" + srcBusCode + ", srcTrxId=" + srcTrxId
					+ ", appId=" + appId + ", busCode=" + busCode + ", callSeq=" + callSeq + ", handler=" + handler
					+ ", calledMethods=" + calledMethods + ", md5=" + md5 + ", syncMethodResult="
					+ Arrays.toString(syncMethodResult) + ", createTime=" + createTime + ", updateTime=" + updateTime
					+ ", lockVersion=" + lockVersion + "]";
		}
	}

	public void saveIdempotentPo(EasyTransFilterChain filterChain, IdempotentPo idempotentPo) {
		JdbcTemplate jdbcTemplate = filterChain.getResource(JDBC_TEMPLATE);
		int update = jdbcTemplate.update(
				insertSql, 
				stringCodecer.findId(APP_ID, idempotentPo.getSrcAppId()),
				stringCodecer.findId(BUSINESS_CODE,idempotentPo.getSrcBusCode()),
				idempotentPo.getSrcTrxId(),
				stringCodecer.findId(APP_ID,idempotentPo.getAppId()),
				stringCodecer.findId(BUSINESS_CODE,idempotentPo.getBusCode()),
				idempotentPo.getCallSeq(),
				stringCodecer.findId(APP_ID,idempotentPo.getHandler()),
				callMethodsString2Id(idempotentPo.getCalledMethods()),
				ObjectDigestUtil.hexStringToByteArray(idempotentPo.getMd5()),
				idempotentPo.getSyncMethodResult(),
				idempotentPo.getCreateTime(),
				idempotentPo.getUpdateTime(),
				idempotentPo.getLockVersion()
				);
		
		if(update != 1){
			throw new RuntimeException("update count exception!" + update);
		}
	}
	
	private String callMethodsString2Id(String stringFormat) {
		String[] strMethods = stringFormat.split(",");
		String[] idMethods = Arrays.stream(strMethods).map(str-> stringCodecer.findId(METHOD_NAME, str)).map(i->i.toString()).toArray(String[]::new);
		return String.join(",", idMethods);
	}
	
	private String id2callMethodsString(String id) {
		String[] strMethods = id.split(",");
		String[] idMethods = Arrays.stream(strMethods).map(str-> stringCodecer.findString(METHOD_NAME, Integer.parseInt(str))).toArray(String[]::new);
		return String.join(",", idMethods);
	}
	
	public void updateIdempotentPo(EasyTransFilterChain filterChain, IdempotentPo idempotentPo) {

		JdbcTemplate jdbcTemplate = filterChain.getResource(JDBC_TEMPLATE);
		int update = jdbcTemplate.update(
				updateSql, 
				callMethodsString2Id(idempotentPo.getCalledMethods()),
				ObjectDigestUtil.hexStringToByteArray(idempotentPo.getMd5()),
				idempotentPo.getSyncMethodResult(),
				idempotentPo.getCreateTime(),
				idempotentPo.getUpdateTime(),
				stringCodecer.findId(APP_ID, idempotentPo.getSrcAppId()),
				stringCodecer.findId(BUSINESS_CODE,idempotentPo.getSrcBusCode()),
				idempotentPo.getSrcTrxId(),
				stringCodecer.findId(APP_ID,idempotentPo.getAppId()),
				stringCodecer.findId(BUSINESS_CODE,idempotentPo.getBusCode()),
				idempotentPo.getCallSeq(),
				stringCodecer.findId(APP_ID,idempotentPo.getHandler()),
				idempotentPo.getLockVersion()
				);
		
		if(update != 1){
			throw new RuntimeException("Optimistic Lock Error Occour Or can not find the specific Record!" + idempotentPo);
		}
	}
}
