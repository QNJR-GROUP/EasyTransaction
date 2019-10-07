package com.yiqiniu.easytrans.extensionsuite.impl.database;

import java.util.concurrent.ConcurrentHashMap;

import com.yiqiniu.easytrans.idgen.TrxIdGenerator;
import com.yiqiniu.easytrans.idgen.impl.SnowFlake;

public class DatabaseSnowFlakeIdGenerator implements TrxIdGenerator {
	
	private long hostSeq;
	private ConcurrentHashMap<String, SnowFlake> mapSnowFlakers = new ConcurrentHashMap<>();

	
	public DatabaseSnowFlakeIdGenerator(long hostSeq) {
	    hostSeq = hostSeq % (2^SnowFlake.MACHINE_BIT);
	}


	@Override
	public long getCurrentTrxId(String busCode) {
		SnowFlake s = mapSnowFlakers.computeIfAbsent(busCode, k->new SnowFlake(hostSeq));
		return s.nextId();
	}
}
