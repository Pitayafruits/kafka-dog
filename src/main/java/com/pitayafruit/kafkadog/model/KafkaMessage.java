package com.pitayafruit.kafkadog.model;

public class KafkaMessage {

    private long offset;
    private String key;
    private String value;
    private long timestamp;
    private boolean consumed;
    private String partition;

    public KafkaMessage(long offset, String key, String value, long timestamp, boolean consumed, String partition) {
        this.offset = offset;
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.consumed = consumed;
        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

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
