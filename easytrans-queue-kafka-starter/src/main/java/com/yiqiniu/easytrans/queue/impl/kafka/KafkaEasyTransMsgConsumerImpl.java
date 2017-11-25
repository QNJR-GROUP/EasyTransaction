package com.yiqiniu.easytrans.queue.impl.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.queue.consumer.EasyTransConsumeAction;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgConsumer;
import com.yiqiniu.easytrans.queue.consumer.EasyTransMsgListener;
import com.yiqiniu.easytrans.queue.impl.kafka.KafkaQueueProperties.ConsumerConfig;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;
import com.yiqiniu.easytrans.util.NamedThreadFactory;

public class KafkaEasyTransMsgConsumerImpl implements EasyTransMsgConsumer {

	private static Logger logger = LoggerFactory.getLogger(KafkaEasyTransMsgConsumerImpl.class);
	private static final String RETRY_COUNT_KEY = "retryCount";
	private static final String ORGINAL_TOPIC = "orginalTopic";
	
	private KafkaConsumer<String, byte[]> consumer;
	private KafkaConsumer<String, byte[]> reconsumer;
	private Thread dispatchThread;
	private Thread reconsumeThread;
	private ConsumerConfig cfg;
	private ExecutorService threadPool;
	private ObjectSerializer serializer;
	private Map<String, EasyTransMsgListener> subscribedKafkaTopics = new ConcurrentHashMap<String, EasyTransMsgListener>(8);
	private HashSet<String> subscribedReconsumeKafkaTopics = new HashSet<String>(4);
	private KafkaEasyTransMsgPublisherImpl retryQueueMsgProducer;
	private Integer[] retryLevelThreshold = null;
	private List</* retry time level */Map<TopicPartition,LinkedList<ConsumerRecord<String, byte[]>>>> retryRecords;
	private Map<String,Integer> retryQueuePartitionCount;

	public KafkaEasyTransMsgConsumerImpl(ConsumerConfig cfg, ObjectSerializer serializer,
			KafkaEasyTransMsgPublisherImpl retryQueueMsgProducer) {
		this.serializer = serializer;
		this.cfg = cfg;
		consumer = new KafkaConsumer<>(cfg.getNativeCfg());
		reconsumer = new KafkaConsumer<>(cfg.getNativeCfg());
		this.retryQueueMsgProducer = retryQueueMsgProducer;
		threadPool = Executors.newFixedThreadPool(cfg.getConsumerThread(), new NamedThreadFactory("KafkaMsgHandler"));

		// 计算每个重试次数对应的重试时间等级阈值
		List<List<Integer>> reconsumeCfg = cfg.getReconsume();
		initRetryThreshold(reconsumeCfg);
		initRetryRecordsMap();
		initRetryQueueSubscribe(reconsumeCfg);
		initRetryQueuePartitionCountMap();
	}

	private void initRetryQueueSubscribe(List<List<Integer>> reconsumeCfg) {
		for(int i = 0; i < reconsumeCfg.size(); i++){
			String topicName = contactRetryTopicName(i);
			subscribedReconsumeKafkaTopics.add(topicName);
		}
		reconsumer.subscribe(subscribedReconsumeKafkaTopics);
	}

	private void initRetryQueuePartitionCountMap() {
		//get topic partition count
		retryQueuePartitionCount = new HashMap<>(4);
		for(String topicName:subscribedReconsumeKafkaTopics){
			List<PartitionInfo> partitionsInfo = reconsumer.partitionsFor(topicName);
			retryQueuePartitionCount.put(topicName, partitionsInfo.size());
		}
	}

	private void initRetryThreshold(List<List<Integer>> reconsumeCfg) {
		retryLevelThreshold = new Integer[reconsumeCfg.size() - 1];
		retryLevelThreshold[0] = reconsumeCfg.get(0).get(1);
		for (int level = 1; level < reconsumeCfg.size() - 1; level++) {
			retryLevelThreshold[level] = retryLevelThreshold[level - 1] + reconsumeCfg.get(level).get(1);
		}
	}

	private void initRetryRecordsMap() {
		retryRecords = new LinkedList<>();
		for(int level = 0; level < this.cfg.getReconsume().size(); level ++){
			retryRecords.add(new HashMap<>());
		}
	}

	@Override
	public synchronized void subscribe(String topic, Collection<String> tags, EasyTransMsgListener listener) {
		for (String tag : tags) {
			String kafkaTopic = QueueKafkaHelper.getKafkaTopic(topic, tag);
			subscribedKafkaTopics.put(kafkaTopic, listener);
		}
		consumer.subscribe(subscribedKafkaTopics.keySet());
	}

	@Override
	public synchronized void start() {
		if (dispatchThread == null) {
			dispatchThread = new Thread("KafkaMessagePollThread") {
				@Override
				public void run() {
					while (true) {
						try {
							pollAndDispatchMessage();
						} catch (InterruptedException e) {
							logger.warn("interuppted,exit KafkaMessagePollThread", e);
							return;
						} catch (Exception e) {
							logger.error("exception occourd in KafkaMessagePollThread!Poll Message 5 seconds later", e);
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e1) {
								logger.warn("interuppted,exit KafkaMessagePollThread", e);
								return;
							}
						}
					}
				}
			};
			dispatchThread.start();
		}
		
		if (reconsumeThread == null) {
			reconsumeThread = new Thread("KafkaReconsumePollThread") {
				@Override
				public void run() {
					while (true) {
						try {
							reconsumeRecords();
						} catch (Exception e) {
							logger.error("KafkaReconsumePollThread Error Occur",e);
							//clean local data and load again
							reconsumer.resume(reconsumer.paused());
							initRetryRecordsMap();//clean and new
							
							try {
								Thread.sleep(5000);
							} catch (InterruptedException ie) {
								logger.info("KafkaReconsumePollThread interrupted,exit thread");
								return;
							}
							
						}
					}
				}
			};
			reconsumeThread.start();
		}
	}

	private void reconsumeRecords() throws InterruptedException {
		ConsumerRecords<String, byte[]> reconsumeRecords = reconsumer.poll(1000);
		
		//add to memory
		Set<TopicPartition> partitions = reconsumeRecords.partitions();
		for(TopicPartition partition:partitions){
			int retryTimeLevel = getRetryTimeLevelFromTopicName(partition.topic());
			Map<TopicPartition, LinkedList<ConsumerRecord<String, byte[]>>> levelRecords = retryRecords.get(retryTimeLevel);
			LinkedList<ConsumerRecord<String, byte[]>> partitionRecords = levelRecords.get(partition);
			if(partitionRecords == null){
				partitionRecords = new LinkedList<>();
				levelRecords.put(partition, partitionRecords);
			}
			List<ConsumerRecord<String, byte[]>> records = reconsumeRecords.records(partition);
			partitionRecords.addAll(records);
		}
		
		//check the records
		int level = 0; 
		List<List<Integer>> reconsumeCfg = cfg.getReconsume();
		//TimeLevel
		for(Map<TopicPartition, LinkedList<ConsumerRecord<String, byte[]>>> levelRecords:retryRecords) {
			//PartitionLevel
			for(Entry<TopicPartition, LinkedList<ConsumerRecord<String, byte[]>>> partitionRecords:levelRecords.entrySet()){
				LinkedList<ConsumerRecord<String, byte[]>> partitonRecordList = partitionRecords.getValue();
				if(partitonRecordList.size() != 0){
					Iterator<ConsumerRecord<String, byte[]>> iterator = partitonRecordList.iterator();
					//RecordLevel
					List<MessageHandler> listJob = new ArrayList<>(4);
					ConsumerRecord<String, byte[]> lastCommitRecord = null;
					while(iterator.hasNext()){
						ConsumerRecord<String, byte[]> record = iterator.next();
						if(System.currentTimeMillis() - record.timestamp() > reconsumeCfg.get(level).get(0)){
							listJob.add(new MessageHandler(record));
							iterator.remove();
							lastCommitRecord = record;
						} else {
							//resume
							break;
						}
					}
					
					executeJobs(listJob);
					
					if(partitonRecordList.size() == 0){
//						 Map<TopicPartition, OffsetAndMetadata> offsets
						reconsumer.commitSync(Collections.singletonMap(partitionRecords.getKey(), new OffsetAndMetadata(lastCommitRecord.offset() + 1)));
						reconsumer.resume(Arrays.asList(partitionRecords.getKey()));
					} else {
						reconsumer.pause(Arrays.asList(partitionRecords.getKey()));
					}
				}
			}
			level++;
		}
	}

	private synchronized void pollAndDispatchMessage() throws InterruptedException {
		// 处理记录过程中，不能修改consumer相关的设定
		// 拉取需要处理的记录
		ConsumerRecords<String, byte[]> allRecords = consumer.poll(10000);

		// 为每个消息都封装成CALLABLE的形式，并进行调用处理
		Iterator<ConsumerRecord<String, byte[]>> iterator = allRecords.iterator();
		List<MessageHandler> listJob = new LinkedList<>();
		while (iterator.hasNext()) {
			listJob.add(new MessageHandler(iterator.next()));
		}
		executeJobs(listJob);
		// 全部调用成功，更新消费坐标
		consumer.commitAsync();
	}

	private void executeJobs(List<MessageHandler> listJob) throws InterruptedException {
		List<Future<ConsumerRecord<String, byte[]>>> invokeAll = threadPool.invokeAll(listJob);

		// 检查每个调用的状态，若调用失败则继续进行调用，直到全部调用完成为止
		for (Future<ConsumerRecord<String, byte[]>> future : invokeAll) {
			Future<ConsumerRecord<String, byte[]>> localFuture = future;
			boolean futureSuccess = false;
			while (!futureSuccess) {
				try {
					// 检测本次的执行结果，若失败则重试
					futureSuccess = localFuture.get() == null;
					if (!futureSuccess) {
						localFuture = threadPool.submit(new MessageHandler(localFuture.get()));
						Thread.sleep(1000);//slow down to avoid continues errors harm
					}
				} catch (ExecutionException e) {
					// 设计中，不会抛出异常
					throw new RuntimeException("Unexpected,it should not throw Exception", e);
				}
			}
		}
	}

	/**
	 * 
	 * @author shirley
	 *
	 */
	private class MessageHandler implements Callable<ConsumerRecord<String, byte[]>> {


		ConsumerRecord<String, byte[]> consumeRecord;

		public MessageHandler(ConsumerRecord<String, byte[]> consumeRecord) {
			super();
			this.consumeRecord = consumeRecord;
		}

		/**
		 * 本方法需要保证不抛出异常，并且在有限时间内幂等执行完成 when success,return null when
		 * failed,return consumeRecord;
		 */
		@Override
		public ConsumerRecord<String, byte[]> call() throws Exception {

			try {
				String orignalTopic = null;
				Iterator<Header> orignalTopicHeaderIterator = consumeRecord.headers().headers(ORGINAL_TOPIC).iterator();
				if(orignalTopicHeaderIterator.hasNext()){
					orignalTopic = serializer.deserialize(orignalTopicHeaderIterator.next().value());
				} else{
					orignalTopic = consumeRecord.topic();
				}
				
				EasyTransMsgListener msgListener = subscribedKafkaTopics.get(orignalTopic);
				Headers headers = consumeRecord.headers();
				HashMap<String, Object> headerMap = new HashMap<>(8);
				Iterator<Header> iterator = headers.iterator();
				while (iterator.hasNext()) {
					Header next = iterator.next();
					headerMap.put(next.key(), serializer.deserialize(next.value()));
				}
				EasyTransConsumeAction consume = msgListener.consume(headerMap,
						serializer.deserialize(consumeRecord.value()));
				if (EasyTransConsumeAction.CommitMessage != consume) {
					reconsumeLater(consumeRecord);
				}
			} catch (Exception e) {
				logger.error("error handling message for topic:" + consumeRecord.topic()
						+ ",sent to retry queue for later proccessing", e);
				try {
					reconsumeLater(consumeRecord);
				} catch (Exception e1) {
					logger.error("publish to reconsume queue failed", e);
					return consumeRecord;
				}
			}

			return null;
		}

	}
	

	private void reconsumeLater(ConsumerRecord<String, byte[]> consumeRecord) throws InterruptedException, ExecutionException {

		// add all header to headList except RETRY_COUNT
		Headers headers = consumeRecord.headers();
		List<Header> headerList = new ArrayList<Header>(8);
		Iterator<Header> iterator = headers.iterator();
		Integer retryCount = -1;
		boolean hasOrignalHeader = false;
		while (iterator.hasNext()) {
			Header next = iterator.next();
			if (next.key().equals(RETRY_COUNT_KEY)) {
				retryCount = serializer.deserialize(next.value());
				continue;
			}
			
			if(next.key().equals(ORGINAL_TOPIC)){
				hasOrignalHeader = true;
			}
			headerList.add(next);
		}
		
		// add RETRY_COUNT to header
		retryCount++;
		headerList.add(new RecordHeader(RETRY_COUNT_KEY, serializer.serialization(retryCount)));
		
		if(!hasOrignalHeader){
			headerList.add(new RecordHeader(ORGINAL_TOPIC, serializer.serialization(consumeRecord.topic())));
		}

		// send message to corresponding queue according to retry times
		String retryTopic = calcRetryTopic(consumeRecord.topic(), retryCount);
		
		ProducerRecord<String, byte[]> record = new ProducerRecord<>(retryTopic,
				consumeRecord.partition() % retryQueuePartitionCount.get(retryTopic), null, consumeRecord.key(),
				consumeRecord.value(), headerList);
		Future<RecordMetadata> publishKafkaMessage = retryQueueMsgProducer.publishKafkaMessage(record);
		publishKafkaMessage.get();
	}

	private String calcRetryTopic(String topic, Integer retryCount) {
		int retryTimeLevel = calcRetryTimeLevel(retryCount);
		return contactRetryTopicName(retryTimeLevel);
	}

	private String contactRetryTopicName(Integer retryTimeLevel) {
		return getConsumerId() + EasytransConstant.EscapeChar + "reconsume" + EasytransConstant.EscapeChar + retryTimeLevel;
	}

	private int calcRetryTimeLevel(Integer retryCount) {
		int level = 0;
		for (; level < retryLevelThreshold.length; level++) {
			if (retryCount < retryLevelThreshold[level]) {
				return level;
			}
		}
		return level;
	}
	

	private int getRetryTimeLevelFromTopicName(String topic) {
		return Integer.parseInt(topic.substring(topic.lastIndexOf(EasytransConstant.EscapeChar) + 1));
	}

	@Override
	public String getConsumerId() {
		return cfg.getNativeCfg().getProperty("group.id");
	}
}
