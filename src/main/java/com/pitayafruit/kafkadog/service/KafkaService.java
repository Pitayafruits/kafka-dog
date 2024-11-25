package com.pitayafruit.kafkadog.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartitionInfo;
import java.util.*;
import java.util.concurrent.ExecutionException;


public class KafkaService {

    public static Map<String, List<TopicPartitionInfo>> getTopicsWithPartitions(String host, int port) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);

        Map<String, List<TopicPartitionInfo>> result = new HashMap<>();

        try (AdminClient adminClient = AdminClient.create(props)) {
            // 获取所有主题
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get();

            // 获取每个主题的分区信息
            for (String topicName : topicNames) {
                DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(
                        Collections.singletonList(topicName)
                );
                TopicDescription topicDescription = describeTopicsResult.values().get(topicName).get();
                result.put(topicName, topicDescription.partitions());
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }
}
