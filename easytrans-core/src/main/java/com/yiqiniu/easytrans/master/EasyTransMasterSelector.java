package com.yiqiniu.easytrans.master;

import java.util.concurrent.TimeUnit;

public interface EasyTransMasterSelector {
	
	/**
	 * whether this job has the leadership
	 * @return
	 */
	boolean hasLeaderShip();
	
	/**
	 * wait till taken LeaderShip or closed.
	 * it should used cooperate with hasLeaderShip
	 * @throws InterruptedException
	 */
	public void await() throws InterruptedException;
	
	/**
	 * Causes the current thread to wait until this instance acquires leadership unless
	 * the thread is interrupted, the specified waiting time elapses or the instance is closed.
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException;
}
