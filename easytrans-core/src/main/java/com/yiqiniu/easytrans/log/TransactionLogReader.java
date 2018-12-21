package com.yiqiniu.easytrans.log;

import java.util.Date;
import java.util.List;

import com.yiqiniu.easytrans.log.vo.LogCollection;

public interface TransactionLogReader {

	/**
	 * 获取当前服务的未完成的日志
	 * get current service's unfinished logs
	 * @param locationId can be null
	 * @param pageSize
	 * @param createTimeCeiling
	 * @return
	 */
	List<LogCollection> getUnfinishedLogs(LogCollection locationId,int pageSize,Date createTimeCeiling);
}
