package com.pitayafruit.kafkadog.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartitionInfo;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class KafkaService {

    public static Map<String, List<TopicPartitionInfo>> getTopicsWithPartitions(String host, int port)
            throws ExecutionException, InterruptedException, TimeoutException {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "3000");

        Map<String, List<TopicPartitionInfo>> result = new HashMap<>();
        AdminClient adminClient = null;

        try {
            adminClient = AdminClient.create(props);
            // 获取所有主题
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get(3, TimeUnit.SECONDS);

            // 获取每个主题的分区信息
            for (String topicName : topicNames) {
                DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(
                        Collections.singletonList(topicName)
                );
                TopicDescription topicDescription = describeTopicsResult.values()
                        .get(topicName).get(3, TimeUnit.SECONDS);
                result.put(topicName, topicDescription.partitions());
            }
            return result;
        } finally {
            if (adminClient != null) {
                adminClient.close();
            }
        }
    }
}
