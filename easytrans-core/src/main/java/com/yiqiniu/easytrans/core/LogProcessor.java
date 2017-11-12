package com.yiqiniu.easytrans.core;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.log.vo.Content;

/**
 * the processor for each log
 */
public interface LogProcessor {
	
	/**
	 * the process method for specified log type
	 * @param ctx log processing context 
	 * @param currentContent processing content
	 * @return true for success,false for end processing and retry later
	 */
	boolean logProcess(LogProcessContext ctx,Content currentContent);
}
