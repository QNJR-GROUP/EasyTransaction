package com.yiqiniu.easytrans.monitor;

/**
 * 
 * @author deyou
 * @date 2019/04/02
 *
 */
public interface MonitorConsumerFactory {

    /**
     * use for methods to call
     * @param monitorInterface
     * @return
     */
    public <T extends EtMonitor> T getRemoteProxy(String appId, Class<T> monitorInterface);

}
