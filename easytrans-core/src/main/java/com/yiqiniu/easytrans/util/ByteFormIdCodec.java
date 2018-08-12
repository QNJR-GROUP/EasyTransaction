package com.yiqiniu.easytrans.util;

import com.yiqiniu.easytrans.protocol.TransactionId;

public interface ByteFormIdCodec {

	byte[] getTransIdByte(TransactionId transId);

	byte[] getAppIdCeil(String appId);

	byte[] getAppIdFloor(String appId);

	TransactionId getTransIdFromByte(byte[] transId);

}