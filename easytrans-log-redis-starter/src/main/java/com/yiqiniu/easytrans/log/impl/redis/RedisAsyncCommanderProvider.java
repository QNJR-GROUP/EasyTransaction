package com.yiqiniu.easytrans.log.impl.redis;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.codec.RedisCodec;

public class RedisAsyncCommanderProvider{
	
	private RedisAsyncCommands<String, byte[]> cmd;
	
	
	public RedisAsyncCommanderProvider(String uri){
		RedisClient client = RedisClient.create(uri);
		StatefulRedisConnection<String, byte[]> connect = client.connect(getCodec());
		cmd = connect.async();
	}
	
	public RedisAsyncCommands<String, byte[]> getAsyncCommand(){
		return cmd;
	}

	private RedisCodec<String, byte[]> getCodec(){
		return new RedisCodec<String,byte[]>(){
			@Override
			public String decodeKey(ByteBuffer bytes) {
				byte[] arr = toByteArray(bytes);
				return new String(arr,StandardCharsets.UTF_8);
			}
			
			@Override
			public ByteBuffer encodeKey(String key) {
				return ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
			}

			@Override
			public byte[] decodeValue(ByteBuffer bytes) {
				return toByteArray(bytes);
			}

			
			@Override
			public ByteBuffer encodeValue(byte[] value) {
				return ByteBuffer.wrap(value);
			}
			
			private byte[] toByteArray(ByteBuffer bytes) {
				byte[] arr = new byte[bytes.remaining()];
				bytes.get(arr);
				return arr;
			}
		};
	}

	
}
