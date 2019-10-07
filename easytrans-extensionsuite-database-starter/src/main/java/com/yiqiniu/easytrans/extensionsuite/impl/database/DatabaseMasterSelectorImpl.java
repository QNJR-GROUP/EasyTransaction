package com.yiqiniu.easytrans.extensionsuite.impl.database;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.yiqiniu.easytrans.master.EasyTransMasterSelector;

/**
 * 利用数据库实现一个简易非严谨,但适用于ET的选主功能
 * @author deyou
 *
 */
public class DatabaseMasterSelectorImpl implements EasyTransMasterSelector {
    
    private Logger logger = LoggerFactory.getLogger(DatabaseMasterSelectorImpl.class);

    private String GET_MAX_INSTANCE_ID = "select ifnull(max(instance_id),0)  max_instance_id  from election where app_id  = ? for update";
    private String INSERT_INSTANCE_CONTROL_LINE = "insert into election values(?, ?,now(),?)";
    private String UPDATE_HEARTBEAT_TIME = "update election set heart_beat_time = now() where app_id = ? and instance_id = ? and TIME_TO_SEC(TIMEDIFF(NOW(), heart_beat_time)) <= ?";
    private String GET_MASTER_INSTANCE = "select min(instance_id) from election where app_id = ? and TIME_TO_SEC(TIMEDIFF(NOW(), heart_beat_time)) <= ?";
    private String CLEAN_EXPIRED_INSTANCE_RECORD = "delete from election where app_id = ? and TIME_TO_SEC(TIMEDIFF(NOW(), heart_beat_time)) > ?";
	
	
    private volatile Integer instanceId;
    private String applicationName;
	private String instanceName;
	private JdbcTemplate jdbcTemplate;
	private PlatformTransactionManager transManager;
	private Integer leaseSeconds;
	
	private Random random;
	
	
	public DatabaseMasterSelectorImpl(String tablePrefix, DataSource dataSoruce, PlatformTransactionManager transManager ,String applicationName,Integer leaseSeconds) {
	    this.instanceName = getHostName().trim() + "-" + getSaltString().trim();
	    this.jdbcTemplate = new JdbcTemplate(dataSoruce);
	    this.random = new Random();
	    this.transManager = transManager;
	    this.applicationName = applicationName;
	    this.leaseSeconds = leaseSeconds;
	    
	    handleTablePrefix(tablePrefix);
	    initInstanceIdAndRecord();
	    
	    ScheduledExecutorService heartBeatThreadPool = Executors.newSingleThreadScheduledExecutor();
	    
	    heartBeatThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateHeartBeat();
            }
        }, leaseSeconds / 2, (leaseSeconds / 3) * 2 + 1, TimeUnit.SECONDS);
	}
	
	private void updateHeartBeat() {
	    try {
	        logger.info("heart beat begin {} {}", applicationName, instanceId);
            int update = jdbcTemplate.update(UPDATE_HEARTBEAT_TIME, applicationName,instanceId,leaseSeconds);
            if(update != 1) {
                logger.error("heart beat failed! use another instanceId!");
                initInstanceIdAndRecord();
            }
            
            int nextInt = random.nextInt(10);
            if(nextInt == 1) {
                logger.info("clean the expired instance recored begin {}", applicationName);
                jdbcTemplate.update(CLEAN_EXPIRED_INSTANCE_RECORD,applicationName,leaseSeconds);
            }
            
        } catch (Exception e) {
            logger.error("heart beat failed!" + instanceId, e);
        }
	}

    private void handleTablePrefix(String tablePrefix) {
        tablePrefix = tablePrefix.trim();
	    
	    if(StringUtils.isNotBlank(tablePrefix)) {
	        GET_MAX_INSTANCE_ID = addTablePrefix(tablePrefix, GET_MAX_INSTANCE_ID);
	        INSERT_INSTANCE_CONTROL_LINE = addTablePrefix(tablePrefix, INSERT_INSTANCE_CONTROL_LINE);
	        UPDATE_HEARTBEAT_TIME = addTablePrefix(tablePrefix, UPDATE_HEARTBEAT_TIME);
	        GET_MASTER_INSTANCE = addTablePrefix(tablePrefix, GET_MASTER_INSTANCE);
	        CLEAN_EXPIRED_INSTANCE_RECORD = addTablePrefix(tablePrefix, CLEAN_EXPIRED_INSTANCE_RECORD);
	    }
    }

    private void initInstanceIdAndRecord() {
        TransactionTemplate transTemplate = new TransactionTemplate(transManager);
	    transTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                
                Integer maxInstanceId = jdbcTemplate.queryForObject(GET_MAX_INSTANCE_ID, Integer.class, new Object[] {applicationName});
                instanceId = maxInstanceId + 1;
                int update = jdbcTemplate.update(INSERT_INSTANCE_CONTROL_LINE, applicationName,instanceId,instanceName);
                if(update != 1) {
                    throw new RuntimeException("insert instance record failed! instanceId:" + maxInstanceId + " updated:" + update);
                }
                return null;
            }
        });
    }

    private String addTablePrefix(String tablePrefix, String sql) {
        return sql.replace("election", tablePrefix + "election");
    }
	
    /**
     * 本方法实现假设：
     * DB与本进程通讯所需时间远小于租约时间，使得与DB交互的处理时间、网络来回时间可忽略不计
     * 
     * heartBeat有效时间内最小的instanceId记录为master
     * 
     */
	@Override
	public boolean hasLeaderShip() {
	    Integer masterInstance = jdbcTemplate.queryForObject(GET_MASTER_INSTANCE, new Object[] {applicationName,leaseSeconds}, Integer.class);
	    if(instanceId.equals(masterInstance)) {	        
	        return true;
	    }
	    return false;
	}

	@Override
	public void await() throws InterruptedException {
        Thread.sleep(getLeaseSeconds() / 2  + random.nextInt(getLeaseSeconds() / 6) - getLeaseSeconds() / 12);
    }

	private int getLeaseSeconds() {
        return leaseSeconds;
    }

    @Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
	    Thread.sleep(unit.toMillis(timeout));
	    return hasLeaderShip();
	}
	
	
	public int getInstanceId() {
        return instanceId;
    }
    
    protected String getSaltString() {
        String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 8) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }
    
    protected String getHostName() {
        String os = System.getProperty("os.name").toLowerCase();

        String hostName = null;
        if (os.contains("win")) {
            hostName = System.getenv("COMPUTERNAME");
            if (StringUtils.isNotBlank(hostName)) {
                return hostName;
            }
            hostName = execReadToString("hostname");
            if (StringUtils.isNotBlank(hostName)) {
                return hostName;
            }
            
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac os x")) {
            
            hostName = System.getenv("HOSTNAME");
            if (StringUtils.isNotBlank(hostName)) {
                return hostName;
            }
            hostName = execReadToString("hostname");
            if (StringUtils.isNotBlank(hostName)) {
                return hostName;
            }
            hostName = execReadToString("cat /etc/hostname");
            if (StringUtils.isNotBlank(hostName)) {
                return hostName;
            }
        } else {
            try {
                return Inet4Address.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return getSaltString();
            }
        }
        
        return getSaltString();
    }

    @SuppressWarnings("resource")
    protected static String execReadToString(String execCommand) {
        try (Scanner s = new Scanner(Runtime.getRuntime().exec(execCommand).getInputStream()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
