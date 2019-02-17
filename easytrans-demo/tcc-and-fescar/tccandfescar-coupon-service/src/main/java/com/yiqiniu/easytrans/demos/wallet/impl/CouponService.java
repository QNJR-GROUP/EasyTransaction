package com.yiqiniu.easytrans.demos.wallet.impl;

import java.util.concurrent.Callable;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yiqiniu.easytrans.core.EasyTransFacade;
import com.yiqiniu.easytrans.demos.wallet.api.vo.UseCouponVO.UseCouponMethodRequest;
import com.yiqiniu.easytrans.demos.wallet.api.vo.UseCouponVO.UseCouponResult;
import com.yiqiniu.easytrans.protocol.autocps.AutoCpsLocalTransactionExecutor;

@Component
public class CouponService {
	
	@Resource
	private EasyTransFacade transaction;
	@Resource
	private JdbcTemplate jdbcTemplate;
	
	
    @Transactional
    public UseCouponResult useCoupon(UseCouponMethodRequest param) {
        
        int update = jdbcTemplate.update("update `coupon` set coupon = coupon - ? where user_id = ? and coupon >= ?;", 
                param.getCoupon(),param.getUserId(),param.getCoupon());
        
        if(update != 1){
            throw new RuntimeException("can not find specific user id or have not enought money");
        }
        
        UseCouponResult userCouponResult = new UseCouponResult();
        userCouponResult.setUsed(param.getCoupon());
        return userCouponResult;
    }
    
    @Transactional
    public int getExcatCouponCount(int userId) throws Exception {
        
        //使用FescarAtLocalTransactionExecutor.executeWithGlobalLockCheck包裹的事务使用 select for update 
        //或者 进行update、delete时，才能保证获取的值为最新，这是fescar的特性导致
        //后续会提供注解形式
        return AutoCpsLocalTransactionExecutor.executeWithGlobalLockCheck(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return jdbcTemplate.queryForObject("select coupon from coupon where user_id = ?", new Object[] {userId}, Integer.class);
            }
        });
        
    }
}
