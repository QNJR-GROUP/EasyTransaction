package com.yiqiniu.easytrans.idgen.impl;

import com.yiqiniu.easytrans.idgen.BusinessCodeGenerator;

public class ConstantBusinessCodeGenerator implements BusinessCodeGenerator {
	@Override
	public String getCurrentBusinessCode() {
		return "default";
	}
}
