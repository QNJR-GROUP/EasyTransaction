package com.yiqiniu.easytrans.rpc.impl.dubbo;

import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.yiqiniu.easytrans.protocol.BusinessIdentifer;

public interface DubboServiceCustomizationer {
	void customDubboService(BusinessIdentifer businessIdentifer,ServiceConfig<GenericService> serviceConfig);
}
