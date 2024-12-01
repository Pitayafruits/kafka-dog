package com.pitayafruit.kafkadog.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartitionInfo;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka集群服务类
 * 负责与Kafka集群交互，提供集群管理和信息查询功能
 * 包括获取主题列表、分区信息等操作
 */
public class KafkaService {

    /**
     * 获取Kafka集群中所有主题及其分区信息
     * 使用Kafka Admin Client API获取集群的主题和分区详细信息
     *
     * @param host Kafka服务器地址
     * @param port Kafka服务器端口
     * @return 包含所有主题及其分区信息的Map，key为主题名称，value为分区信息列表
     * @throws ExecutionException 执行Admin Client操作时可能发生的异常
     * @throws InterruptedException 线程中断时可能发生的异常
     * @throws TimeoutException 操作超时时抛出的异常
     */
    public static Map<String, List<TopicPartitionInfo>> getTopicsWithPartitions(String host, int port)
            throws ExecutionException, InterruptedException, TimeoutException {
        // 配置Admin Client属性
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);
        // 设置请求超时时间为3秒
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        // 设置API调用超时时间为3秒
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "3000");

        Map<String, List<TopicPartitionInfo>> result = new HashMap<>();
        AdminClient adminClient = null;

        try {
            // 创建Admin Client实例
            adminClient = AdminClient.create(props);
            // 获取所有主题
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get(3, TimeUnit.SECONDS);

            // 获取每个主题的分区信息
            for (String topicName : topicNames) {
                // 查询主题详细信息
                DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(
                        Collections.singletonList(topicName)
                );
                // 获取主题描述信息，包括分区数据
                TopicDescription topicDescription = describeTopicsResult.values()
                        .get(topicName).get(3, TimeUnit.SECONDS);
                // 将主题及其分区信息添加到结果Map中
                result.put(topicName, topicDescription.partitions());
            }
            return result;
        } finally {
            // 确保Admin Client资源被正确释放
            if (adminClient != null) {
                adminClient.close();
            }
        }
    }
}
