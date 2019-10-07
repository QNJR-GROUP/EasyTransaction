package com.yiqiniu.easytrans.idgen.impl;

public class SnowFlake {
    /**
     * modified from https://github.com/beyondfengyu/SnowFlake
     */


    /**
     * 每一部分占用的位数
     */
    private final static long TIMESTAMP_SECOND_BIT = 32;   
    private final static long SEQUENCE_BIT = 16; //序列号占用的位数 4096
    public final static long MACHINE_BIT = 64 - TIMESTAMP_SECOND_BIT - SEQUENCE_BIT;   //机器标识占用的位数 32
    /**
     * 每一部分的最大值
     */
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long TIMESTMP_LEFT = SEQUENCE_BIT;
    private final static long MACHINE_LEFT  = TIMESTMP_LEFT + MACHINE_BIT;

    private long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastStmp = -1L;//上一次时间戳

    public SnowFlake(long machineId) {
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     *
     * @return
     */
    public synchronized long nextId() {
        long currStmp = getCurrentSecond();
        if (currStmp < lastStmp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currStmp == lastStmp) {
            //相同秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一秒的序列数已经达到最大
            if (sequence == 0L) {
                throw new RuntimeException("max tp/ms reached!");
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStmp = currStmp;
        
        return machineId << MACHINE_LEFT // 机器标识部分
                | currStmp << TIMESTMP_LEFT // 时间戳部分
                | sequence;                 //序列号部分
    }


    private long getCurrentSecond() {
        long mill = System.currentTimeMillis() / 1000;
        return mill;
    }


    
    
    
}
