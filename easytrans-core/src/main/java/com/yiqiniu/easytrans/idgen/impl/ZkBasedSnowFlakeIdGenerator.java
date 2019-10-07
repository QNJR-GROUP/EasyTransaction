package com.yiqiniu.easytrans.idgen.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.apache.zookeeper.CreateMode;

import com.yiqiniu.easytrans.idgen.TrxIdGenerator;

public class ZkBasedSnowFlakeIdGenerator implements TrxIdGenerator {
	
	private long hostSeq = 0;
	private ConcurrentHashMap<String, SnowFlake> mapSnowFlakers = new ConcurrentHashMap<>();
	
	public ZkBasedSnowFlakeIdGenerator(String zooKeeperUrl, String applicationName) {
		CuratorFramework client = CuratorFrameworkFactory.newClient(zooKeeperUrl, new RetryForever(1000));
		client.start();
		try {
			String path = "/EasyTransIdGen/" + applicationName + "/P";
			String nodeName = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
			String sequenceStr = nodeName.replaceAll(path, "");
			hostSeq = Long.parseLong(sequenceStr) % (2^SnowFlake.MACHINE_BIT);
			client.close();//do not need to keep connection, hostSeq will not change
		} catch (Exception e) {
			throw new RuntimeException("create Id generator failed",e);
		}
	}


	@Override
	public long getCurrentTrxId(String busCode) {
		SnowFlake s = mapSnowFlakers.computeIfAbsent(busCode, k->new SnowFlake(hostSeq));
		return s.nextId();
	}
	
	


}
