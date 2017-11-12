package com.yiqiniu.easytrans.protocol;

public interface BusinessProvider<P extends EasyTransRequest<?, ?>>{
	
	/**
	 * Idempotent implement by Framework code<br/>
	 * this will take extract performance cost,but it will help decrease the complexity of business<br/>
	 */
	public static final int IDENPOTENT_TYPE_FRAMEWORK = 0;
	
	/**
	 * Idempotent implement by Business code<br/>
	 * this will take extract develop cost in business,but it will help increase the performance
	 */
	public static final int IDENPOTENT_TYPE_BUSINESS = 1;
	
	/**
	 * idempotent type declare. 
	 * IDENPOTENT_TYPE_FRAMEWORK = 0
	 * IDENPOTENT_TYPE_BUSINESS = 1
	 * @return
	 */
	int getIdempotentType();

}
