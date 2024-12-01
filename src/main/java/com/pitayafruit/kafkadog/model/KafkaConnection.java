package com.pitayafruit.kafkadog.model;

/**
 * Kafka连接配置实体类
 * 用于存储和管理Kafka服务器的连接信息
 */
public class KafkaConnection {

    /**
     * Kafka连接名称
     */
    private String name;

    /**
     * Kafka服务器主机地址
     */
    private String host;

    /**
     * Kafka服务器端口号
     */
    private int port;

    /**
     * 连接配置的唯一标识ID
     */
    private String id;

    /**
     * 无参构造函数
     */
    public KafkaConnection() {}

    /**
     * 带参构造函数
     * @param name Kafka连接名称
     * @param host Kafka服务器主机地址
     * @param port Kafka服务器端口号
     */
    public KafkaConnection(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    // Getters and Setters
    /**
     * 获取连接名称
     * @return 连接名称
     */
    public String getName() { return name; }

    /**
     * 设置连接名称
     * @param name 连接名称
     */
    public void setName(String name) { this.name = name; }

    /**
     * 获取主机地址
     * @return 主机地址
     */
    public String getHost() { return host; }

    /**
     * 设置主机地址
     * @param host 主机地址
     */
    public void setHost(String host) { this.host = host; }

    /**
     * 获取端口号
     * @return 端口号
     */
    public int getPort() { return port; }

    /**
     * 设置端口号
     * @param port 端口号
     */
    public void setPort(int port) { this.port = port; }

    /**
     * 获取连接ID
     * @return 连接ID
     */
    public String getId() { return id; }

    /**
     * 设置连接ID
     * @param id 连接ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * 重写toString方法,返回连接的字符串表示形式
     * @return 格式化的连接信息字符串: "名称 (主机:端口)"
     */
    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }
}