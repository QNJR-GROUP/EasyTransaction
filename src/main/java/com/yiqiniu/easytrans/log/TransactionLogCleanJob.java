package com.yiqiniu.easytrans.log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.yiqiniu.easytrans.config.EasyTransConifg;
import com.yiqiniu.easytrans.master.EasyTransMasterSelector;
import com.yiqiniu.easytrans.util.NamedThreadFactory;

public class TransactionLogCleanJob {
	
	@Resource
	private EasyTransConifg config;
	
	@Resource
	private EasyTransMasterSelector master;
	
	@Resource
	private TransactionLogWritter logWritter;
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	@PostConstruct
	public void init(){
		String days = config.getExtendConfig("translog.clean.before.days");
		
		if(StringUtils.isEmpty(days)){
			LOG.warn("translog.clean.before.days not set. do not execute clean job");
			return;
		}
		
		String cleanTime = config.getExtendConfig("translog.clean.time").trim();
		final Integer cleanLogDaysRestriction = Integer.valueOf(days);
		Date nextExeucteTime = calcNextExecuteTime(cleanTime);
		long initialDelay = nextExeucteTime.getTime() - System.currentTimeMillis();
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("CleanLogJob",true));
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try{
					if(master.hasLeaderShip()){
						Calendar instance = Calendar.getInstance();
						instance.add(Calendar.DATE, -cleanLogDaysRestriction);
						LOG.info("START CLEAN EXPIRED TRANSACTION LOGS.DAYS:" + cleanLogDaysRestriction);
						logWritter.cleanFinishedLogs(config.getAppId(), instance.getTime());
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
