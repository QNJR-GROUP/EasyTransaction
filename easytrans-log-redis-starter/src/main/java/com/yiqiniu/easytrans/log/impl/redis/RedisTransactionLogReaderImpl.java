package com.yiqiniu.easytrans.log.impl.redis;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.lambdaworks.redis.Limit;
import com.lambdaworks.redis.Range;
import com.lambdaworks.redis.Range.Boundary;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.yiqiniu.easytrans.log.TransactionLogReader;
import com.yiqiniu.easytrans.log.vo.Content;
import com.yiqiniu.easytrans.log.vo.LogCollection;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.ByteFormIdCodec;
import com.yiqiniu.easytrans.util.ObjectDigestUtil;

public class RedisTransactionLogReaderImpl implements TransactionLogReader {

	private String appId;
	private RedisAsyncCommands<String, byte[]> async;
	private ObjectSerializer objectSerializer;
	private String keyPrefix;
	private ByteFormIdCodec idCodec;

	public RedisTransactionLogReaderImpl(String appId, RedisAsyncCommanderProvider cmdProvider,
			ObjectSerializer objectSerializer, String keyPrefix, ByteFormIdCodec idCodec) {
		this.async = cmdProvider.getAsyncCommand();
		this.appId = appId;
		this.objectSerializer = objectSerializer;
		this.keyPrefix = keyPrefix;
		this.idCodec = idCodec;
	}

	private static class Pair<K, V> {

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		K key;
		V value;

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<LogCollection> getUnfinishedLogs(LogCollection locationId, int pageSize, Date createTimeCeiling) {

		double latestTime = createTimeCeiling.getTime();
		double floorTime = Double.MIN_VALUE;
		if (locationId != null) {
			floorTime = locationId.getCreateTime().getTime();
		}

		RedisFuture<List<ScoredValue<byte[]>>> zrangebyscore = async.zrangebyscoreWithScores(keyPrefix + appId,
				Range.from(Boundary.excluding(floorTime), Boundary.including(latestTime)),
				Limit.create(0, pageSize));

		List<LogCollection> result = new ArrayList<>();
		try {
			List<ScoredValue<byte[]>> transIds = zrangebyscore.get();
			if (transIds == null || transIds.size() == 0) {
				return Collections.emptyList();
			}
			
			//turn bytes to String
			List<Pair<String, Double>> transIdStrs = transIds.stream()
					.map(item -> new Pair<>(new String(item.value, StandardCharsets.UTF_8), item.score))
					.collect(Collectors.toList());
			
			//get search future list
			Map<String, Pair<RedisFuture<List<byte[]>>, Double>> collect = transIdStrs.stream()
					.collect(Collectors.toMap(pair -> pair.getKey(),
							pair -> new Pair<>(async.lrange(pair.getKey(), 0, -1), pair.getValue())));

			for (Entry<String, Pair<RedisFuture<List<byte[]>>, Double>> entry : collect.entrySet()) {

				TransactionId splitTransId = idCodec.getTransIdFromByte(
						ObjectDigestUtil.hexStringToByteArray(entry.getKey().substring(keyPrefix.length())));
				Pair<RedisFuture<List<byte[]>>, Double> value = entry.getValue();
				List<byte[]> byteArrayList = value.getKey().get();
				List<Content> contentList = byteArrayList.stream().map(objectSerializer::<Content>deserialize)
						.collect(Collectors.toList());
				LogCollection log = new LogCollection(
						splitTransId.getAppId(),
						splitTransId.getBusCode(),
						splitTransId.getTrxId(),
						new ArrayList(contentList), new Date(value.getValue().longValue()));

				result.add(log);
			}

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		return result;
	}
}
