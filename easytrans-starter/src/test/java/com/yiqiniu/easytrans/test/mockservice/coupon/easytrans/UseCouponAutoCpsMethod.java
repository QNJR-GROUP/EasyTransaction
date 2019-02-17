package com.yiqiniu.easytrans.test.mockservice.coupon.easytrans;

import java.io.Serializable;

import javax.annotation.Resource;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.autocps.AutoCpsMethodRequest;
import com.yiqiniu.easytrans.test.Constant;
import com.yiqiniu.easytrans.test.mockservice.coupon.CouponService;

public class UseCouponAutoCpsMethod{

    public static final String METHOD_NAME="useCoupon";
	
	@Resource
	private CouponService couponService;


//    @Override
//    protected UseCouponResult doBusiness(UseCouponMethodRequest param) {
//        return couponService.useCoupon(param);
//    }
    

	public static class UseCouponResult implements Serializable{
		private static final long serialVersionUID = 1L;
		private Long used;
        public Long getUsed() {
            return used;
        }
        public void setUsed(Long used) {
            this.used = used;
        }
	}
	
	@BusinessIdentifer(appId=Constant.APPID,busCode=METHOD_NAME,rpcTimeOut=2000)
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
	}

}
