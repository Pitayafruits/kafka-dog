package com.pitayafruit.kafkadog.controller;

import com.pitayafruit.kafkadog.model.KafkaConnection;
import com.pitayafruit.kafkadog.service.ConnectionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;

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
        // 加载保存的连接
        connections.addAll(ConnectionManager.loadConnections());
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
        showConnectionDialog(null);
    }

    private void editConnection() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showConnectionDialog(selected);
        }
    }

    private void showConnectionDialog(KafkaConnection connection) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/connection_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(connection == null ? "添加连接" : "编辑连接");
            stage.setScene(new Scene(root));

            ConnectionDialogController controller = loader.getController();
            controller.setMainController(this);
            if (connection != null) {
                controller.setConnection(connection);
            }

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void deleteConnection() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除这个连接吗？");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                connections.remove(selected);
                ConnectionManager.deleteConnection(selected);
            }
        }
    }

    public void addConnection(KafkaConnection connection) {
        ConnectionManager.saveConnection(connection);

        // 如果是编辑现有连接，先移除旧的
        connections.removeIf(c -> c.getId() != null && c.getId().equals(connection.getId()));
        connections.add(connection);
    }
}