package com.yiqiniu.easytrans.util;

import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.APP_ID;
import static com.yiqiniu.easytrans.core.EasytransConstant.StringCodecKeys.BUSINESS_CODE;

import java.nio.ByteBuffer;

import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.stringcodec.StringCodec;

/**
 * 
 * turn the id to the byte form
 * 
 * @author xudeyou
 */
public class DeafultByteFormIdCodec implements ByteFormIdCodec {

	private StringCodec codecer;

	public DeafultByteFormIdCodec(StringCodec codecer) {
		this.codecer = codecer;
	}

	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.util.ByteFormIdCodec#getTransIdByte(com.yiqiniu.easytrans.protocol.TransactionId)
	 */
	@Override
	public byte[] getTransIdByte(TransactionId transId) {

		int appIdCode = codecer.findId(APP_ID, transId.getAppId()).intValue();
		int busCodeId = codecer.findId(BUSINESS_CODE, transId.getBusCode()).intValue();

		ByteBuffer byteBuffer = ByteBuffer.allocate(12);
		putUnsignedShort(byteBuffer, appIdCode);
		putUnsignedShort(byteBuffer, busCodeId);
		byteBuffer.putLong(transId.getTrxId());
		
		return byteBuffer.array();
	}
	
	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.util.ByteFormIdCodec#getAppIdCeil(java.lang.String)
	 */
	@Override
	public byte[] getAppIdCeil(String appId) {

		int appIdCode = codecer.findId(APP_ID, appId).intValue();
		ByteBuffer byteBuffer = ByteBuffer.allocate(12);
		putUnsignedShort(byteBuffer, appIdCode);
		putUnsignedShort(byteBuffer, Integer.MAX_VALUE);
		putUnsignedShort(byteBuffer, Integer.MAX_VALUE);
		putUnsignedShort(byteBuffer, Integer.MAX_VALUE);
		putUnsignedShort(byteBuffer, Integer.MAX_VALUE);
		putUnsignedShort(byteBuffer, Integer.MAX_VALUE);
		
		return byteBuffer.array();
	}
	
	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.util.ByteFormIdCodec#getAppIdFloor(java.lang.String)
	 */
	@Override
	public byte[] getAppIdFloor(String appId) {

		int appIdCode = codecer.findId(APP_ID, appId).intValue();
		ByteBuffer byteBuffer = ByteBuffer.allocate(12);
		putUnsignedShort(byteBuffer, appIdCode);
		putUnsignedShort(byteBuffer, 0);
		putUnsignedShort(byteBuffer, 0);
		putUnsignedShort(byteBuffer, 0);
		putUnsignedShort(byteBuffer, 0);
		putUnsignedShort(byteBuffer, 0);
		
		return byteBuffer.array();
	}

	/* (non-Javadoc)
	 * @see com.yiqiniu.easytrans.util.ByteFormIdCodec#getTransIdFromByte(byte[])
	 */
	@Override
	public TransactionId getTransIdFromByte(byte[] transId) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(transId);
		int appId = getUnsignedShort(byteBuffer);
		int busCode = getUnsignedShort(byteBuffer);
		long trxId = byteBuffer.getLong();
		
		return new TransactionId(codecer.findString(APP_ID, appId), codecer.findString(BUSINESS_CODE, busCode), trxId);
	}

	private int getUnsignedShort(ByteBuffer bb) {
		return (bb.getShort() & 0xffff);
	}

	private void putUnsignedShort(ByteBuffer bb, int value) {
		bb.putShort((short) (value & 0xffff));
	}

}
