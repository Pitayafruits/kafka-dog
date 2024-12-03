package com.pitayafruit.kafkadog.service;

import com.pitayafruit.kafkadog.model.KafkaMessage;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Kafka消息服务类
 * 负责从Kafka主题分区中预览和获取消息
 */
public class KafkaMessageService {

    /**
     * 预览指定主题分区的消息
     * 支持从不同位置（earliest、latest、指定offset）开始获取消息
     *
     * @param host Kafka服务器地址
     * @param port Kafka服务器端口
     * @param topic 主题名称
     * @param partition 分区号
     * @param offset 起始偏移量（"earliest"、"latest"或具体的偏移量）
     * @param pageSize 每页消息数量
     * @return 消息列表
     */
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

            // 先获取消费者组的提交偏移量
            Map<TopicPartition, Long> committedOffsets = new HashMap<>();
            try (AdminClient adminClient = AdminClient.create(props)) {
                ListConsumerGroupsResult groups = adminClient.listConsumerGroups();
                Collection<ConsumerGroupListing> groupsList;
                try {
                    groupsList = groups.all().get(3, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to get consumer groups", e);
                } catch (java.util.concurrent.TimeoutException e) {
                    throw new RuntimeException("Timeout while getting consumer groups", e);
                }

                for (ConsumerGroupListing group : groupsList) {
                    ListConsumerGroupOffsetsResult offsetsResult =
                            adminClient.listConsumerGroupOffsets(group.groupId());
                    Map<TopicPartition, OffsetAndMetadata> groupOffsets;
                    try {
                        groupOffsets = offsetsResult.partitionsToOffsetAndMetadata().get(3, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Failed to get consumer group offsets", e);
                    } catch (java.util.concurrent.TimeoutException e) {
                        throw new RuntimeException("Timeout while getting consumer group offsets", e);
                    }

                    if (groupOffsets.containsKey(topicPartition)) {
                        committedOffsets.put(topicPartition,
                                groupOffsets.get(topicPartition).offset());
                    }
                }
            }

            // 设置消息起始位置
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
                    // 检查消息是否已被消费
                    boolean isConsumed = false;
                    for (Long committedOffset : committedOffsets.values()) {
                        if (record.offset() < committedOffset) {
                            isConsumed = true;
                            break;
                        }
                    }

                    messages.add(new KafkaMessage(
                            record.offset(),
                            record.key(),
                            record.value(),
                            record.timestamp(),
                            isConsumed,  // 使用实际的消费状态
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