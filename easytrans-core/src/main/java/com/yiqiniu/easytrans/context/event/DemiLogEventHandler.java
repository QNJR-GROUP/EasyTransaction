package com.yiqiniu.easytrans.context.event;

import com.yiqiniu.easytrans.context.LogProcessContext;
import com.yiqiniu.easytrans.log.vo.Content;

public interface DemiLogEventHandler{
	
	/**
	 * 需要匹配出现的日志，成功匹配
	 * @param logCtx
	 * @param content
	 * @return 返回ture表示执行成功，返回false表示执行失败，等待下次重试
	 */
	boolean onMatch(LogProcessContext logCtx, Content leftContent,Content rightContent);
	
	/**
	 * 需要匹配出现的日志，匹配失败
	 * @param logCollection
	 * @return 返回ture表示执行成功，返回false表示执行失败，等待下次重试
	 */
	boolean onDismatch(LogProcessContext logCtx, Content leftContent);
}