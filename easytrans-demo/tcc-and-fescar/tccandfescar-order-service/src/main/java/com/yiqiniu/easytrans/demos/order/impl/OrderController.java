package com.yiqiniu.easytrans.demos.order.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OrderController {
	
	@Autowired
	private OrderService orderService;
	
	@RequestMapping("/buySth")
	@ResponseBody
	public Integer buySomething(@RequestParam int userId,@RequestParam int money){
		return orderService.buySomething(userId, money);
	}
}
