package com.yiqiniu.easytrans.test.mockservice.express;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.AfterMasterTransMethodResult;
import com.yiqiniu.easytrans.test.mockservice.express.easytrans.ExpressDeliverAfterTransMethod.ExpressDeliverAfterTransMethodRequest;

@Component
public class ExpressService {
	
	@Resource
	private TestUtil util;
	
	public AfterMasterTransMethodResult afterTransaction(ExpressDeliverAfterTransMethodRequest param) {
		AfterMasterTransMethodResult afterMasterTransMethodResult = new AfterMasterTransMethodResult();
		afterMasterTransMethodResult.setMessage(callExternalServiceForPickupCargo(param));
		return afterMasterTransMethodResult;
	}

	public int getUserExpressCount(int userId){
		
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,ExpressDeliverAfterTransMethod.BUSINESS_CODE,(ExpressDeliverAfterTransMethodRequest)null);
		Integer queryForObject = jdbcTemplate.queryForObject("select count(1) from express where user_id = ?", Integer.class,userId);
		if(queryForObject == null){
			queryForObject = 0;
		}
		return queryForObject;
	}

	private String callExternalServiceForPickupCargo(ExpressDeliverAfterTransMethodRequest param) {
		//pretend to be a external service
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,ExpressDeliverAfterTransMethod.BUSINESS_CODE,param);
		TransactionId trxId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
		
		int update = jdbcTemplate.update("INSERT INTO `express` (`p_app_id`, `p_bus_code`, `p_trx_id`, `user_id`) VALUES (?, ?, ?, ?);", 
				trxId.getAppId(),trxId.getBusCode(),trxId.getTrxId(),param.getUserId());
		
		if(update != 1){
			throw new RuntimeException("unkonwn Exception!");
		}
		
		return "your cargo will be depart in 10 minutes ";
	}
	
	
}
