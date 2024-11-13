package com.pitayafruit.kafkadog.model;

public class KafkaConnection {
    private String name;
    private String host;
    private int port;
    private String id;

    public KafkaConnection() {}

    public KafkaConnection(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }
}