package com.pitayafruit.kafkadog.controller;

import com.pitayafruit.kafkadog.model.KafkaConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {
    @FXML
    private ListView<KafkaConnection> connectionListView;
    @FXML
    private ListView<String> topicListView;
    @FXML
    private ListView<String> messageListView;

    private ObservableList<KafkaConnection> connections = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        connectionListView.setItems(connections);
        setupContextMenu();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("编辑");
        MenuItem deleteItem = new MenuItem("删除");

        editItem.setOnAction(e -> editConnection());
        deleteItem.setOnAction(e -> deleteConnection());

        contextMenu.getItems().addAll(editItem, deleteItem);
        connectionListView.setContextMenu(contextMenu);
    }

    @FXML
    private void showAddConnectionDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/connection_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("添加连接");
            stage.setScene(new Scene(root));

            ConnectionDialogController controller = loader.getController();
            controller.setMainController(this);

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editConnection() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 实现编辑逻辑
        }
    }

    private void deleteConnection() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            connections.remove(selected);
        }
    }

    public void addConnection(KafkaConnection connection) {
        connections.add(connection);
    }
}