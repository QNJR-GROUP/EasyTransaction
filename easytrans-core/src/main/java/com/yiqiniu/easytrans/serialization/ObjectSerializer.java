package com.yiqiniu.easytrans.serialization;

public interface ObjectSerializer {
	byte[] serialization(Object obj);
	<T> T deserialize(byte[] bytes);
}
