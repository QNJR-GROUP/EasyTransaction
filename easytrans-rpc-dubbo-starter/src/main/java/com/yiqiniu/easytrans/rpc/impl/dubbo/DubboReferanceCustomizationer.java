package com.yiqiniu.easytrans.rpc.impl.dubbo;

import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.rpc.service.GenericService;

public interface DubboReferanceCustomizationer {
	void customDubboReferance(String appId, String busCode,ReferenceConfig<GenericService> referenceConfig);
}
