package com.yiqiniu.easytrans.idgen;

public interface TrxIdGenerator {
	long getCurrentTrxId(String busCode);
}
