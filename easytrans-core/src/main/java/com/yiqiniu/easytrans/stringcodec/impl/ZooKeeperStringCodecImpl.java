package com.yiqiniu.easytrans.stringcodec.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.RetryForever;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.stringcodec.StringCodec;

public class ZooKeeperStringCodecImpl implements StringCodec {
	
	private static Logger LOG = LoggerFactory.getLogger(ZooKeeperStringCodecImpl.class);
		
	private String zooKeeperUrl;
	private String applicationName;
	private String appBaseUrl;
	private String systemBaseUrl;
	private CuratorFramework client;
	private TreeCache treeCache;
	private ConcurrentHashMap<String/*type*/, ConcurrentHashMap<String,Integer>> mapStr2Id = new ConcurrentHashMap<>(8);
	private ConcurrentHashMap<String/*type*/, ConcurrentHashMap<Integer,String>> mapId2Str = new ConcurrentHashMap<>(8);
	
	
	public ZooKeeperStringCodecImpl(String zooKeeperUrl, String applicationName) {
		super();
		this.zooKeeperUrl = zooKeeperUrl;
		this.applicationName = applicationName;
		this.appBaseUrl = "/EasyTransStringCodec"+"/" + this.applicationName;
		this.systemBaseUrl = "/EasyTransStringCodec/_system";
		init();
	}

	private void init(){
		client = CuratorFrameworkFactory.newClient(zooKeeperUrl, new RetryForever(1000));
		client.start();
		
		try {
			treeCache =  TreeCache.newBuilder(client, appBaseUrl).setCacheData(true).build();
			treeCache.getListenable().addListener(new TreeCacheListener() {
				@Override
				public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
					
					if(event.getType() != Type.NODE_ADDED) {
						return;
					}
					String path = event.getData().getPath();
					if(!path.startsWith(appBaseUrl)) {
						throw new RuntimeException("unknown error ,path invalid:" + path);
					}
					
					path = path.replaceAll(appBaseUrl + "/", "");
					String[] split = path.split("/");
					if(split.length != 2) {
						return;
					}
					
					String strType = split[0];
					String strValue = split[1];
					int id = ByteBuffer.wrap(event.getData().getData()).getInt();
					
					ConcurrentHashMap<String, Integer> mapTypeStr2Id = mapStr2Id.computeIfAbsent(strType, k -> new ConcurrentHashMap<>());
					mapTypeStr2Id.put(strValue, id);
					
					ConcurrentHashMap<Integer, String> mapTypeId2Str = mapId2Str.computeIfAbsent(strType, k -> new ConcurrentHashMap<>());
					mapTypeId2Str.put(id, strValue);
				}
			});
			
			treeCache.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Integer findId(String stringType, String value) {
		
		if(value == null) {
			return null;
		}
		
		for(int i = 0; i < 3; i++) {
			ConcurrentHashMap<String, Integer> typeMap = mapStr2Id.computeIfAbsent(stringType, key->new ConcurrentHashMap<>());
			Integer id = typeMap.get(value);
			if(id != null) {
				return id;
			} 
			
			register(stringType,value);
			try {
				//wait for async call back to update typeMap
				Thread.sleep((i+1) * 500);
			} catch (InterruptedException e) {
				throw new RuntimeException("exit findId method",e);
			}
		}
		
		throw new RuntimeException("get id failed!" + stringType + "," + value);
	}

	private void register(String stringType, String value) {

		
		try {
			
			//can not switch the order of sysStat and appStat.because of the zk sequential consistency
			Stat sysStat = client.checkExists().creatingParentContainersIfNeeded().forPath(getSystemTypePath(stringType));
			Stat appStringStat = client.checkExists().forPath(getAppBaseItemPath(stringType, value));
			if(appStringStat != null) {
				return;
			}
			
			if(sysStat == null) {
				createNodeIfNotExists(getSystemTypePath(stringType));
			}
			createNodeIfNotExists(getAppTypePath(stringType));
			
			int numChildren = 0;
			if(sysStat != null) {
				numChildren = sysStat.getNumChildren();
			}
			client.inTransaction()
			.create().forPath(getSystemBaseItemPath(stringType, numChildren + 1),value.getBytes()).and()
			.create().forPath(getAppBaseItemPath(stringType, value),ByteBuffer.allocate(4).putInt(numChildren + 1).array()).and()
			.commit();
			
		} catch (Exception e) {
			LOG.warn("register string in zk failed", e);
		}
	
	}

	private void createNodeIfNotExists(String path) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path);
		} catch (NodeExistsException e) {
			LOG.info("node exists:" + path);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getSystemTypePath(String stringType) {
		return systemBaseUrl + "/" + stringType;
	}
	
	public String getAppTypePath(String stringType) {
		return appBaseUrl + "/" + stringType;
	}

	public String getSystemBaseItemPath(String stringType, int numChildren) {
		return systemBaseUrl + "/" + stringType + "/" + numChildren;
	}

	public String getAppBaseItemPath(String stringType, String string) {
		return appBaseUrl + "/" + stringType + "/" + string;
	}

	@Override
	public String findString(String stringType, int id) {
		for(int i = 0; i < 3; i++) {
			ConcurrentHashMap<Integer, String> typeMap = mapId2Str.computeIfAbsent(stringType, key->new ConcurrentHashMap<>());
			String idStr = typeMap.get(id);
			if(idStr != null) {
				return idStr;
			} 
			
			try {
				//wait and retry
				Thread.sleep((i+1) * 500);
			} catch (InterruptedException e) {
				throw new RuntimeException("exit findId method",e);
			}
		}
		throw new RuntimeException("get string failed!" + stringType + "," + id);
	}
}
