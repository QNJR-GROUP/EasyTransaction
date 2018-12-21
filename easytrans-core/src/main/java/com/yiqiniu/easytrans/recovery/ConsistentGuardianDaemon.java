package com.yiqiniu.easytrans.recovery;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.core.ConsistentGuardian;
import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.master.EasyTransMasterSelector;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.util.NamedThreadFactory;

public class ConsistentGuardianDaemon {
	
	private TransactionLogReader logReader;
	private ConsistentGuardian guardian;
	private EasyTransMasterSelector master;
	private Integer executeInterval;
	private Integer pageSize;
	private Integer delay;
	
	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private HashMap<TransactionId, Object[]/*lastRetryTime,retried count*/> mapRetryInfo = new HashMap<TransactionId, Object[]>();
	
	public ConsistentGuardianDaemon(TransactionLogReader logReader, ConsistentGuardian guardian,
			EasyTransMasterSelector master, Integer executeInterval, Integer pageSize, Integer delay) {
		super();
		this.logReader = logReader;
		this.guardian = guardian;
		this.master = master;
		this.executeInterval = executeInterval;
		this.pageSize = pageSize;
		this.delay = delay;
		init();
	}

	private void init(){
		
		ThreadFactory threadFactory = new NamedThreadFactory("ConsistentGuardianJob", false);
		Runnable run = new Runnable() {
			@Override
			public void run() {

				try {
					master.await();
					if (!master.hasLeaderShip()) {
						mapRetryInfo.clear();
						return;
					}

					LogCollection locationId = null;
					List<LogCollection> collections = null;
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.SECOND, -delay);
					do {
						collections = logReader.getUnfinishedLogs(locationId, pageSize, calendar.getTime());
						for (LogCollection logCollection : collections) {
							if (checkRetry(logCollection)) {
								try {
									updateRetryInfo(logCollection, guardian.process(logCollection));
								} catch (Exception e) {
									updateRetryInfo(logCollection, false);
									LOG.error("Consistent handle failed!", e);
								}
								LOG.info(
										"End finish handling transaction {} {} {}",
										logCollection.getAppId(),
										logCollection.getBusCode(),
										logCollection.getTrxId());
							} else {
								LOG.info(
										"Skip transaction {} {} {},for retry time limit",
										logCollection.getAppId(),
										logCollection.getBusCode(),
										logCollection.getTrxId());
							}

						}
						
						if(collections != null && collections.size() != 0){
							locationId = collections.get(collections.size() - 1);
						}else{
							locationId = null;
						}
						
					} while (collections != null && collections.size() != 0	&& master.hasLeaderShip());
				} catch(InterruptedException e){
					LOG.warn("Interrupte recived,end consisten guadian");
					
				} catch (Exception e) {
					LOG.error("Consistent Guardian Deamon Error",e);
				}
			}
		};
		
		Executors.newSingleThreadScheduledExecutor(threadFactory).scheduleWithFixedDelay(
				run, 
				10, 
				executeInterval, 
				TimeUnit.SECONDS);
	}
	
	private void updateRetryInfo(LogCollection logCollection,boolean success) {
		TransactionId id = new TransactionId(logCollection.getAppId(), logCollection.getBusCode(), logCollection.getTrxId());
		if(success){
			mapRetryInfo.remove(id);
		}else{
			Object[] objects = mapRetryInfo.get(id);
			if(objects == null){
				objects = new Object[]{new Date(),1};
				mapRetryInfo.put(id, objects);
			}else{
				objects[0] = new Date();
				objects[1] = ((Integer)objects[1]) + 1;
			}
		}
	}

	Calendar instance = Calendar.getInstance();
	private boolean checkRetry(LogCollection logCollection) {
		TransactionId id = new TransactionId(logCollection.getAppId(), logCollection.getBusCode(), logCollection.getTrxId());
		Object[] objects = mapRetryInfo.get(id);
		if(objects == null){
			return true;
		}
		
		Date lastTryTime = (Date) objects[0];
		Integer triedCount = (Integer) objects[1];
		instance.setTimeInMillis(lastTryTime.getTime());
		instance.add(Calendar.SECOND, (int)Math.pow(2, (triedCount + 1)));
		
		long nextExecuteTime = instance.getTimeInMillis();
		
		if(nextExecuteTime < System.currentTimeMillis()){
			return true;
		}
		
		return false;
	}
	
}
