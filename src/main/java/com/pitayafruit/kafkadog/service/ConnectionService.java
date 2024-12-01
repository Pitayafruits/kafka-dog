package com.pitayafruit.kafkadog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitayafruit.kafkadog.model.KafkaConnection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kafka连接管理服务类
 * 负责Kafka连接配置的持久化存储和管理
 * 包括连接的保存、加载、删除等操作
 */
public class ConnectionService {

    /**
     * 连接配置文件路径
     * 用于存储所有Kafka连接配置的JSON文件
     */
    private static final String CONNECTIONS_FILE = "connections.json";

    /**
     * JSON对象映射器
     * 用于处理连接配置的序列化和反序列化
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 将连接配置列表保存到文件
     * @param connections 要保存的连接配置列表
     */
    public static void saveConnections(List<KafkaConnection> connections) {
        try {
            mapper.writeValue(new File(CONNECTIONS_FILE), connections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载连接配置列表
     * @return 加载的连接配置列表，如果加载失败则返回空列表
     */
    public static List<KafkaConnection> loadConnections() {
        try {
            File file = new File(CONNECTIONS_FILE);
            if (file.exists()) {
                return mapper.readValue(file, new TypeReference<>() {
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 保存单个连接配置
     * 如果是新连接则添加到列表，如果是已存在的连接则更新
     * @param connection 要保存的连接配置
     */
    public static void saveConnection(KafkaConnection connection) {
        List<KafkaConnection> connections = loadConnections();
        if (connection.getId() == null) {
            // 新连接，生成UUID并添加到列表
            connection.setId(UUID.randomUUID().toString());
            connections.add(connection);
        } else {
            // 更新已存在的连接
            for (int i = 0; i < connections.size(); i++) {
                if (connections.get(i).getId().equals(connection.getId())) {
                    connections.set(i, connection);
                    break;
                }
            }
        }
        saveConnections(connections);
    }

    /**
     * 删除指定的连接配置
     * @param connection 要删除的连接配置
     */
    public static void deleteConnection(KafkaConnection connection) {
        List<KafkaConnection> connections = loadConnections();
        connections.removeIf(c -> c.getId().equals(connection.getId()));
        saveConnections(connections);
    }
}