package com.yiqiniu.easytrans.context.event;

import com.yiqiniu.easytrans.context.LogProcessContext;


public interface GuardianProcessEndEventHandler{
	/**
	 * 所有Log对应操作及登记的其他事件都执行完毕后，执行本方法
	 * @param logCollection
	 * @return 返回ture表示执行成功，返回false表示执行失败，等待下次重试
	 */
	boolean beforeProcessEnd(LogProcessContext logCollection);
}