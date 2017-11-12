package com.yiqiniu.easytrans.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public class DefaultEasyTransFilterFactory implements EasyTransFilterChainFactory {

	private List<EasyTransFilter> defaultFilters;

	public DefaultEasyTransFilterFactory(List<EasyTransFilter> defaultFilters) {
		super();
		this.defaultFilters = defaultFilters;
	}

	@Override
	public EasyTransFilterChain getDefaultFilterChain(String appId,	String busCode, String innerMethod) {
		return new InnerFilterChainImpl(appId, busCode, innerMethod, getDefaultFilters());
	}

	@Override
	public EasyTransFilterChain getFilterChainByFilters(String appId,
			String busCode, String innerMethod, List<EasyTransFilter> filters) {
		return new InnerFilterChainImpl(appId, busCode, innerMethod, filters);
	}

	
	@Override
	public List<EasyTransFilter> getDefaultFilters() {
		return new ArrayList<EasyTransFilter>(defaultFilters);
	}
	
	private static class InnerFilterChainImpl implements EasyTransFilterChain{

		private String appId;
		private String busCode;
		private String innerMethodName;
		private List<EasyTransFilter> listFilter;
		private int pos = 0;
		private HashMap<String,Object> context = new HashMap<String, Object>();
		
		public InnerFilterChainImpl(String appId, String busCode,
				String innerMethodName, List<EasyTransFilter> listFilter) {
			super();
			this.appId = appId;
			this.busCode = busCode;
			this.innerMethodName = innerMethodName;
			this.listFilter = listFilter;
		}

		@Override
		public String getAppId() {
			return appId;
		}

		@Override
		public String getBusCode() {
			return busCode;
		}

		@Override
		public String getInnerMethodName() {
			return innerMethodName;
		}

		@Override
		public void addFilter(EasyTransFilter filter) {
			listFilter.add(filter);
		}

		@Override
		public EasyTransResult invokeFilterChain(Map<String,Object> header,EasyTransRequest<?, ?> request) {
			if(pos >= listFilter.size()){
				throw new RuntimeException("reach the filter end!" + request);
			}
			
			EasyTransFilter easyTransFilter = listFilter.get(pos++);
			return easyTransFilter.invoke(this, header, request);
		}

		@Override
		public Object bindResource(String key, Object resource) {
			return context.put(key, resource);
		}


		@SuppressWarnings("unchecked")
		@Override
		public <T> T getResource(String key) {
			return (T) context.get(key);
		}
		
	}

}
