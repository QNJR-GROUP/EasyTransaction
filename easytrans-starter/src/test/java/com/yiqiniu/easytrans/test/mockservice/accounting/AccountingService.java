package com.yiqiniu.easytrans.test.mockservice.accounting;

import java.util.Date;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.filter.MetaDataFilter;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.protocol.cps.EtCps;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingRequest;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingRequestCfg;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingResponse;
import com.yiqiniu.easytrans.test.mockservice.order.OrderService;

@Component
public class AccountingService {
	
	@Resource
	private TestUtil util;
	
	public int getTotalCost(int userId){
		JdbcTemplate jdbcTemplate = getJdbcTemplate(null);
		Integer queryForObject = jdbcTemplate.queryForObject("SELECT sum(amount) FROM `accounting` where user_id = ?;", Integer.class, userId);
		return queryForObject==null?0:queryForObject;
	}
	
	@EtCps(cancelMethod="reverseEntry", idempotentType=BusinessProvider.IDENPOTENT_TYPE_FRAMEWORK,cfgClass=AccountingRequestCfg.class)
	public AccountingResponse accounting(AccountingRequest param) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate(param);
		
		TransactionId trxId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
		
		int update = jdbcTemplate.update("INSERT INTO `accounting` (`accounting_id`, `p_app_id`, `p_bus_code`, `p_trx_id`, `user_id`, `amount`, `create_time`) VALUES (NULL, ?, ?, ?, ?, ?, ?);",
				trxId.getAppId(),
				trxId.getBusCode(),
				trxId.getTrxId(),
				param.getUserId(),
				param.getAmount(),
				new Date());
		
		if(update != 1){
			throw new RuntimeException("unkonw Exception!");
		}
		return new AccountingResponse();
	}

	public void reverseEntry(AccountingRequest param) {
	    

        OrderService.checkThrowException(OrderService.EXCEPTION_TAG_IN_MIDDLE_OF_CONSISTENT_GUARDIAN_WITH_ROLLEDBACK_MASTER_TRANS);
    
		JdbcTemplate jdbcTemplate = getJdbcTemplate(param);
		
		TransactionId trxId = MetaDataFilter.getMetaData(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
		
		int update = jdbcTemplate.update("INSERT INTO `accounting` (`accounting_id`, `p_app_id`, `p_bus_code`, `p_trx_id`, `user_id`, `amount`, `create_time`) VALUES (NULL, ?, ?, ?, ?, ?, ?);",
				trxId.getAppId(),
				trxId.getBusCode(),
				trxId.getTrxId(),
				param.getUserId(),
				-param.getAmount(),
				new Date());
		
		if(update != 1){
			throw new RuntimeException("unkonw Exception!");
		}
	}

	private JdbcTemplate getJdbcTemplate(AccountingRequest param) {
		return util.getJdbcTemplate(Constant.APPID,AccountingCpsMethod.METHOD_NAME,(EasyTransRequest<?, ?>) param);
	}

}
