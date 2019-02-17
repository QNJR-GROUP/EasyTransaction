package com.yiqiniu.easytrans.demos.wallet.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.yiqiniu.easytrans.demos.wallet.api.vo.UseCouponVO.UseCouponMethodRequest;
import com.yiqiniu.easytrans.demos.wallet.api.vo.UseCouponVO.UseCouponResult;
import com.yiqiniu.easytrans.protocol.autocps.AbstractAutoCpsMethod;

@Component
public class UseCouponAutoCpsService extends AbstractAutoCpsMethod<UseCouponMethodRequest,UseCouponResult>{

	@Resource
	private CouponService couponService;

	@Override
	public int getIdempotentType() {
		return IDENPOTENT_TYPE_FRAMEWORK;
	}

    @Override
    protected UseCouponResult doBusiness(UseCouponMethodRequest param) {
        return couponService.useCoupon(param);
    }
}
