package com.yiqiniu.easytrans.demos.wallet.api.vo;

import java.io.Serializable;

import com.yiqiniu.easytrans.demos.wallet.api.CouponServiceApiConstant;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.autocps.AutoCpsMethodRequest;

public class UseCouponVO {  public static class UseCouponResult implements Serializable{
    private static final long serialVersionUID = 1L;
    private Long used;
    public Long getUsed() {
        return used;
    }
    public void setUsed(Long used) {
        this.used = used;
    }
}

@BusinessIdentifer(appId=CouponServiceApiConstant.APPID,busCode="useCoupon",rpcTimeOut=2000)
public static class UseCouponMethodRequest implements AutoCpsMethodRequest<UseCouponResult>{

    private static final long serialVersionUID = 1L;
    
    private Integer userId;
    
    private Long coupon;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Long getCoupon() {
        return coupon;
    }

    public void setCoupon(Long coupon) {
        this.coupon = coupon;
    }
}}
