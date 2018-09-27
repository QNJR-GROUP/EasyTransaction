package com.yiqiniu.easytrans.queue.impl.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yiqiniu.easytrans.core.EasytransConstant;
import com.yiqiniu.easytrans.protocol.TransactionId;
import com.yiqiniu.easytrans.queue.impl.kafka.KafkaQueueProperties.ProducerConfig;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublishResult;
import com.yiqiniu.easytrans.queue.producer.EasyTransMsgPublisher;
import com.yiqiniu.easytrans.serialization.ObjectSerializer;

public class KafkaEasyTransMsgPublisherImpl implements EasyTransMsgPublisher {
	
	private static Logger log = LoggerFactory.getLogger(KafkaEasyTransMsgPublisherImpl.class);
	
	private Producer<String,byte[]> kafkaProducer;
	private ObjectSerializer serializer;
	private ProducerConfig cfg;
	
	public KafkaEasyTransMsgPublisherImpl(ProducerConfig cfg,ObjectSerializer serializer){
		this.serializer = serializer;
		this.cfg = cfg;
		kafkaProducer = new KafkaProducer<String,byte[]>(this.cfg.getNativeCfg());
	}

	
	@Override
	public EasyTransMsgPublishResult publish(String topic, String tag, String key, Map<String,Object> header, byte[] msgByte) {
		String kafkaTopic = QueueKafkaHelper.getKafkaTopic(topic, tag);
		
		//calculate partition
		TransactionId trxId = (TransactionId) header.get(EasytransConstant.CallHeadKeys.PARENT_TRX_ID_KEY);
		int partition = calcMessagePartition(kafkaTopic, trxId);
		
		List<Header> kafkaHeaderList = new ArrayList<>(header.size());
		for(Entry<String, Object> entry:header.entrySet()){
			kafkaHeaderList.add(new RecordHeader(entry.getKey(),serializer.serialization(entry.getValue())));
		}
		
		ProducerRecord<String, byte[]> record = new ProducerRecord<>(kafkaTopic, partition, null, key, msgByte, kafkaHeaderList);
		Future<RecordMetadata> sendResultFuture = kafkaProducer.send(record);
		try {
			RecordMetadata recordMetadata = sendResultFuture.get();
			log.info("message sent:" + recordMetadata);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("message sent error",e);
		}
		
		EasyTransMsgPublishResult easyTransMsgPublishResult = new EasyTransMsgPublishResult();
		easyTransMsgPublishResult.setTopic(topic);
		easyTransMsgPublishResult.setMessageId(key);
		return easyTransMsgPublishResult;
	}


	public int calcMessagePartition(String kafkaTopic, TransactionId trxId) {
		List<PartitionInfo> partitionMetaData = kafkaProducer.partitionsFor(kafkaTopic);
		int partitionSize = partitionMetaData.size();
		int partition = Math.abs(trxId.hashCode() % partitionSize);
		return partition;
	}
	
	public Future<RecordMetadata> publishKafkaMessage(ProducerRecord<String,byte[]> record){
		return kafkaProducer.send(record);
	}
}
