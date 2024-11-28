package com.pitayafruit.kafkadog.service;

import com.pitayafruit.kafkadog.model.KafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class KafkaMessageService {

    public static List<KafkaMessage> previewMessages(String host, int port, String topic,
                                                     int partition, String offset, int pageSize) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-dog-preview-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<KafkaMessage> messages = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            TopicPartition topicPartition = new TopicPartition(topic, partition);
            consumer.assign(Collections.singletonList(topicPartition));

            // 设置起始位置
            long targetOffset;
            if (offset.equals("earliest")) {
                consumer.seekToBeginning(Collections.singletonList(topicPartition));
                targetOffset = consumer.position(topicPartition);
            } else if (offset.equals("latest")) {
                consumer.seekToEnd(Collections.singletonList(topicPartition));
                targetOffset = Math.max(0, consumer.position(topicPartition) - pageSize);
                consumer.seek(topicPartition, targetOffset);
            } else {
                targetOffset = Long.parseLong(offset);
                consumer.seek(topicPartition, targetOffset);
            }

            // 拉取指定数量的消息
            int messageCount = 0;
            while (messageCount < pageSize) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                if (records.isEmpty()) break;

                for (ConsumerRecord<String, String> record : records) {
                    messages.add(new KafkaMessage(
                            record.offset(),
                            record.key(),
                            record.value(),
                            record.timestamp(),
                            false,
                            topic + "-" + partition
                    ));

                    messageCount++;
                    if (messageCount >= pageSize) break;
                }
            }
        }
        return messages;
    }
}
