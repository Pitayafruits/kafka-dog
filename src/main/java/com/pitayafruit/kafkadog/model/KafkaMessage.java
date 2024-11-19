package com.pitayafruit.kafkadog.model;

/**
 * Kafka消息实体类
 */
public class KafkaMessage {

    /**
     * 消息在分区中的偏移量
     */
    private long offset;

    /**
     * 消息的key
     */
    private String key;

    /**
     * 消息的实际内容
     */
    private String value;

    /**
     * 消息的时间戳
     */
    private long timestamp;

    /**
     * 消息是否已被消费的标志
     */
    private boolean consumed;

    /**
     * 消息所在的分区
     */
    private String partition;

    /**
     * 全参数构造函数
     * @param offset 消息偏移量
     * @param key 消息key
     * @param value 消息内容
     * @param timestamp 时间戳
     * @param consumed 是否已消费
     * @param partition 所在分区
     */
    public KafkaMessage(long offset, String key, String value, long timestamp, boolean consumed, String partition) {
        this.offset = offset;
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.consumed = consumed;
        this.partition = partition;
    }

    /**
     * 获取消息偏移量
     * @return 消息偏移量
     */
    public long getOffset() {
        return offset;
    }

    /**
     * 设置消息偏移量
     * @param offset 消息偏移量
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * 获取消息key
     * @return 消息key
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置消息key
     * @param key 消息key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取消息内容
     * @return 消息内容
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置消息内容
     * @param value 消息内容
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * 获取消息时间戳
     * @return 消息时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 设置消息时间戳
     * @param timestamp 消息时间戳
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 判断消息是否已被消费
     * @return 消费状态
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * 设置消息消费状态
     * @param consumed 消费状态
     */
    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    /**
     * 获取消息所在分区
     * @return 分区信息
     */
    public String getPartition() {
        return partition;
    }

    /**
     * 设置消息所在分区
     * @param partition 分区信息
     */
    public void setPartition(String partition) {
        this.partition = partition;
    }

    /**
     * 重写toString方法,返回格式化的消息信息字符串
     * @return 格式化的消息信息,包含分区、偏移量、时间、Key、Value和消费状态
     */
    @Override
    public String toString() {
        return String.format("[%s] Offset: %d, Time: %s, Key: %s, Value: %s %s",
                partition,
                offset,
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp)),
                key,
                value,
                consumed ? "[已消费]" : "[未消费]"
        );
    }
}