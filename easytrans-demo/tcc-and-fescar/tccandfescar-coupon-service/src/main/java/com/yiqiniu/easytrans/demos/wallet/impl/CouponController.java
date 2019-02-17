package com.yiqiniu.easytrans.demos.wallet.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CouponController {
	
	@Autowired
	private CouponService couponService;
	
	@RequestMapping("/couponCount")
	@ResponseBody
	public Integer buySomething(@RequestParam int userId) throws Exception{
		return couponService.getExcatCouponCount(userId);
	}
}
