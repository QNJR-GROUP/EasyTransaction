package com.yiqiniu.easytrans.filter;

import java.io.Serializable;


public class EasyTransResult implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Object result;

    private Throwable exception;

    public EasyTransResult(){
    }

    public EasyTransResult(Object result){
        this.result = result;
    }

    public EasyTransResult(Throwable exception){
        this.exception = exception;
    }

    public Object recreate() throws Throwable {
        if (exception != null) {
            throw exception;
        }
        return result;
    }

	public Object getValue() {
        return result;
    }

    public void setValue(Object value) {
        this.result = value;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable e) {
        this.exception = e;
    }

    public boolean hasException() {
        return exception != null;
    }

    @Override
    public String toString() {
        return "RpcResult [result=" + result + ", exception=" + exception + "]";
    }
}