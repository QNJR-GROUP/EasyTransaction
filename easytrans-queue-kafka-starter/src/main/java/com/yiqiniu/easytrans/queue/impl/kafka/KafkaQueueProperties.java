package com.yiqiniu.easytrans.queue.impl.kafka;

import java.util.List;
import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xudeyou
 */

@ConfigurationProperties(prefix = "easytrans.queue.kafka")
public class KafkaQueueProperties {
	private Boolean enabled;
	private ProducerConfig produerCfg;
	private ConsumerConfig consumerCfg;
	
	public ProducerConfig getProduerCfg() {
		return produerCfg;
	}

	public void setProduerCfg(ProducerConfig produerCfg) {
		this.produerCfg = produerCfg;
	}

	public ConsumerConfig getConsumerCfg() {
		return consumerCfg;
	}

	public void setConsumerCfg(ConsumerConfig consumerCfg) {
		this.consumerCfg = consumerCfg;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
	public static class ProducerConfig{
		private Properties nativeCfg;

		public Properties getNativeCfg() {
			return nativeCfg;
		}

		public void setNativeCfg(Properties nativeCfg) {
			this.nativeCfg = nativeCfg;
		}
		
		
	}
	
	public static class ConsumerConfig{
		private Properties nativeCfg;
		private int consumerThread = Runtime.getRuntime().availableProcessors(); 
		private List<List<Integer>> reconsume;

		public List<List<Integer>> getReconsume() {
			return reconsume;
		}

		public void setReconsume(List<List<Integer>> reconsume) {
			this.reconsume = reconsume;
		}

		public int getConsumerThread() {
			return consumerThread;
		}

		public void setConsumerThread(int consumerThread) {
			this.consumerThread = consumerThread;
		}

		public Properties getNativeCfg() {
			return nativeCfg;
		}

		public void setNativeCfg(Properties nativeCfg) {
			this.nativeCfg = nativeCfg;
		}
		
	}
}
