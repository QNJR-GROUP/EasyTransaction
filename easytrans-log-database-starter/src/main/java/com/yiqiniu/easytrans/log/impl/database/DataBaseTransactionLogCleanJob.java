package com.yiqiniu.easytrans.log.impl.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.master.EasyTransMasterSelector;
import com.yiqiniu.easytrans.util.NamedThreadFactory;

public class DataBaseTransactionLogCleanJob {
	
	private EasyTransMasterSelector master;
	private DataBaseTransactionLogWritterImpl logWritter;
	private int logReservedDays;
	private String logCleanTime;
	private String applicationName;
	
	public DataBaseTransactionLogCleanJob(String applicationName,EasyTransMasterSelector master, DataBaseTransactionLogWritterImpl logWritter,
			int logReservedDays, String logCleanTime) {
		super();
		this.master = master;
		this.logWritter = logWritter;
		this.logReservedDays = logReservedDays;
		this.logCleanTime = logCleanTime;
		this.applicationName = applicationName;
		
		init();
	}

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	public void init(){
		String cleanTime = logCleanTime;
		Date nextExeucteTime = calcNextExecuteTime(cleanTime);
		long initialDelay = nextExeucteTime.getTime() - System.currentTimeMillis();
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("CleanLogJob",true));
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try{
					if(master.hasLeaderShip()){
						Calendar instance = Calendar.getInstance();
						instance.add(Calendar.DATE, -logReservedDays);
						LOG.info("START CLEAN EXPIRED TRANSACTION LOGS.DAYS:" + logReservedDays);
						logWritter.cleanFinishedLogs(applicationName, instance.getTime());
						LOG.info("END CLEAN EXPIRED TRANSACTION LOGS.DAYS");
					}else{
						LOG.info("NOT MASTER,do not execute transaction log clean job");
					}
				}catch(Exception e){
					LOG.error("execute clean job error!",e);
				}
			}
		}, initialDelay, 24l*60*60*1000 , TimeUnit.MILLISECONDS);
	}

	private static Date calcNextExecuteTime(String cleanTime) {
		SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat wholeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date nextExeucteTime;
		try {
			String dayString = dayFormatter.format(new Date());
			nextExeucteTime = wholeFormatter.parse(dayString + " " + cleanTime);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		if(nextExeucteTime.getTime() < System.currentTimeMillis()){
			Calendar instance = Calendar.getInstance();
			instance.setTime(nextExeucteTime);
			instance.add(Calendar.DATE, 1);
			nextExeucteTime = instance.getTime();
		}
		
		return nextExeucteTime;
	}
}
