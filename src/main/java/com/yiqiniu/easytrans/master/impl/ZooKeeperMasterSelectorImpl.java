package com.yiqiniu.easytrans.master.impl;

import java.io.EOFException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.RetryForever;

import com.yiqiniu.easytrans.config.EasyTransConifg;
import com.yiqiniu.easytrans.master.EasyTransMasterSelector;

public class ZooKeeperMasterSelectorImpl implements EasyTransMasterSelector {
	
	@Resource
	private EasyTransConifg config;
	
	private LeaderLatch leaderLatch;
	
	@PostConstruct
	private void init(){
		CuratorFramework client = CuratorFrameworkFactory.newClient(config.getExtendConfig("freestanding.zookeeper.url"), new RetryForever(1000));
		leaderLatch = new LeaderLatch(client, "/EasyTransMasterSelector"+"/" + config.getAppId());
		try {
			client.start();
			leaderLatch.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean hasLeaderShip() {
		return leaderLatch.hasLeadership();
	}

	@Override
	public void await() throws InterruptedException {
		try {
			leaderLatch.await();
		} catch (EOFException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return leaderLatch.await(timeout,unit);
	}

}
