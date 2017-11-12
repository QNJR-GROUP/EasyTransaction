package com.yiqiniu.easytrans.log.vo;



/**
 * 需要找到匹配的另外半边的LOG
 */
public abstract class DemiLeftContent extends Content {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * 用于记录同一事务里，同一远程调用方法的调用顺序号
	 */
	private Integer callSeq;

	public Integer getCallSeq() {
		return callSeq;
	}

	public void setCallSeq(Integer callSeq) {
		this.callSeq = callSeq;
	}
	
	
}
