package com.yiqiniu.easytrans.provider.factory;

import java.util.List;
import java.util.Set;

public interface ListableProviderFactory {

	Set<Class<?>> getServiceRootKey();

	Set<Class<?>> getServiceTransactionTypeSet(Class<?> rootType);

	List<Object> getServices(Class<?> root, Class<?> transactionType);
	
	Object getService(String appId,String busCode);
	
	Class<?> getServiceInterface(String appId,String busCode);

}