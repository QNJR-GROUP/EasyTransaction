package com.yiqiniu.easytrans.serialization.impl;

import org.springframework.util.SerializationUtils;

import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class SpringObjectSerialization implements ObjectSerializer{
	
	@Override
	public byte[] serialization(Object obj) {
		return SerializationUtils.serialize(obj);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes) {
		return (T)SerializationUtils.deserialize(bytes);
	}

}
