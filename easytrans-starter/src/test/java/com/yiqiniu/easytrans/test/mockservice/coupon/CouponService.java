package com.yiqiniu.easytrans.test.mockservice.coupon;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.TestUtil;
import com.yiqiniu.easytrans.test.mockservice.coupon.easytrans.UseCouponFescarAtMethod;
import com.yiqiniu.easytrans.test.mockservice.coupon.easytrans.UseCouponFescarAtMethod.UseCouponMethodRequest;
import com.yiqiniu.easytrans.test.mockservice.coupon.easytrans.UseCouponFescarAtMethod.UseCouponResult;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod;
import com.yiqiniu.easytrans.test.mockservice.wallet.easytrans.WalletPayTccMethod.WalletPayTccMethodRequest;

@Component
public class CouponService {
	
	@Resource
	private TestUtil util;

	@Resource
	private EasyTransFacade transaction;
	
	public int getUserCoupon(int userId){
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,WalletPayTccMethod.METHOD_NAME,(WalletPayTccMethodRequest)null);
		Integer queryForObject = jdbcTemplate.queryForObject("select coupon from coupon where user_id = ?", Integer.class, userId);
		return queryForObject == null?0:queryForObject;
	}
	
	@Transactional(transactionManager="useCouponTransactionManager")
	public UseCouponResult useCoupon(UseCouponMethodRequest param) {
	    
		JdbcTemplate jdbcTemplate = util.getJdbcTemplate(Constant.APPID,UseCouponFescarAtMethod.METHOD_NAME,param);
		int update = jdbcTemplate.update("update `coupon` set coupon = coupon - ? where user_id = ? and coupon >= ?;", 
				param.getCoupon(),param.getUserId(),param.getCoupon());
		
		if(update != 1){
			throw new RuntimeException("can not find specific user id or have not enought money");
		}
		
		UseCouponResult userCouponResult = new UseCouponResult();
		userCouponResult.setUsed(param.getCoupon());
		return userCouponResult;
	}
	


}
