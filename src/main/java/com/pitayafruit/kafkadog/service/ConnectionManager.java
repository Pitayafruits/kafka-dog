package com.pitayafruit.kafkadog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitayafruit.kafkadog.model.KafkaConnection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class ConnectionManager {

    private static final String CONNECTIONS_FILE = "connections.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void saveConnections(List<KafkaConnection> connections) {
        try {
            mapper.writeValue(new File(CONNECTIONS_FILE), connections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<KafkaConnection> loadConnections() {
        try {
            File file = new File(CONNECTIONS_FILE);
            if (file.exists()) {
                return mapper.readValue(file, new TypeReference<List<KafkaConnection>>() {});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void saveConnection(KafkaConnection connection) {
        List<KafkaConnection> connections = loadConnections();
        if (connection.getId() == null) {
            connection.setId(UUID.randomUUID().toString());
            connections.add(connection);
        } else {
            // Update existing connection
            for (int i = 0; i < connections.size(); i++) {
                if (connections.get(i).getId().equals(connection.getId())) {
                    connections.set(i, connection);
                    break;
                }
            }
        }
        saveConnections(connections);
    }

    public static void deleteConnection(KafkaConnection connection) {
        List<KafkaConnection> connections = loadConnections();
        connections.removeIf(c -> c.getId().equals(connection.getId()));
        saveConnections(connections);
    }

}
