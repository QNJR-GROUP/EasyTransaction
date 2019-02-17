package com.yiqiniu.easytrans.protocol;

public interface RequestClassAware {
    
    public static final String GET_REQUEST_CLASS = "getRequestClass";
    
    Class<? extends EasyTransRequest<?, ?>> getRequestClass();
}
