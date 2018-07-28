package com.yiqiniu.easytrans.log.impl.redis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.yiqiniu.easytrans.core.EasyTransStaticHelper;
import com.yiqiniu.easytrans.log.TransactionLogWritter;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class RedisTransactionLogWritterImpl implements TransactionLogWritter {

	private RedisAsyncCommands<String, byte[]> async;
	private ObjectSerializer objectSerializer;
	private String keyPrefix;

	public RedisTransactionLogWritterImpl(RedisAsyncCommanderProvider cmdProvider, ObjectSerializer objectSerializer, String keyPrefix) {
		this.async = cmdProvider.getAsyncCommand();
		this.objectSerializer = objectSerializer;
		this.keyPrefix = keyPrefix;
	}

	@Override
	public void appendTransLog(String appId, String busCode, String trxId, List<Content> newOrderedContent,
			boolean finished) {

		String transId = EasyTransStaticHelper.getTransId(appId, busCode, trxId);
		RedisFuture<Long> zadd = null;
		RedisFuture<Long> rpush = null;
		RedisFuture<Long> zrem = null;
		RedisFuture<Long> del = null;
		if (!finished) {
			zadd = async.zadd(keyPrefix + appId, (double) System.currentTimeMillis(), (keyPrefix + transId).getBytes(StandardCharsets.UTF_8));
			//序列化
			byte[][] array = newOrderedContent.stream().map(objectSerializer::serialization).toArray(byte[][]::new);
			rpush = async.rpush(keyPrefix + transId, array);
		} else {
			zrem = async.zrem(keyPrefix + appId, (keyPrefix + transId).getBytes(StandardCharsets.UTF_8));
			del = async.del(keyPrefix + transId);
		}

		try {
			if (zadd != null) {
				zadd.get();
			}
			if (rpush != null) {
				rpush.get();
			}
			if (zrem != null) {
				zrem.get();
			}
			if (del != null) {
				del.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

}
