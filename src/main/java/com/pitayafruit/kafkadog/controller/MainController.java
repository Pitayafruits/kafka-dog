package com.pitayafruit.kafkadog.controller;


import com.pitayafruit.kafkadog.model.KafkaConnection;
import com.pitayafruit.kafkadog.service.ConnectionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.pitayafruit.kafkadog.service.KafkaService;
import javafx.scene.control.*;
import org.apache.kafka.common.TopicPartitionInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 主界面控制器类
 * 负责管理Kafka连接、主题显示和消息查看
 */
public class MainController {
    @FXML
    private VBox leftVBox;

    @FXML
    private TreeView<String> connectionTreeView;

    private TreeItem<String> lastExpandedItem = null;

    private ContextMenu connectionContextMenu;

    @FXML
    public void initialize() {
        loadSavedConnections();
        setupTreeViewListener();
        setupContextMenu();
    }

    private void setupContextMenu() {
        connectionContextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("编辑");
        editItem.setOnAction(event -> handleEditConnection());

        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(event -> handleDeleteConnection());

        connectionContextMenu.getItems().addAll(editItem, deleteItem);
    }

    private void setupTreeViewListener() {
        connectionTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getParent() == connectionTreeView.getRoot()) {
                handleConnectionSelection(newValue);
            }
        });

        // 添加鼠标事件监听器
        connectionTreeView.setOnMouseClicked(event -> {
            TreeItem<String> selectedItem = connectionTreeView.getSelectionModel().getSelectedItem();
            if (event.getButton() == MouseButton.SECONDARY && // 右键点击
                    selectedItem != null &&
                    selectedItem.getParent() == connectionTreeView.getRoot()) { // 确保点击的是连接节点

                connectionContextMenu.show(connectionTreeView, event.getScreenX(), event.getScreenY());
            } else {
                connectionContextMenu.hide();
            }
        });
    }

    private void handleEditConnection() {
        TreeItem<String> selectedItem = connectionTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            KafkaConnection connection = findConnectionByTreeItem(selectedItem);
            if (connection != null) {
                showEditDialog(connection);
            }
        }
    }

    private void showEditDialog(KafkaConnection connection) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/connection_dialog.fxml"));
            Parent root = loader.load();

            ConnectionDialogController controller = loader.getController();
            controller.setMainController(this);
            controller.setConnectionForEdit(connection); // 需要在ConnectionDialogController中添加此方法

            Stage dialogStage = new Stage();
            dialogStage.setTitle("编辑Kafka连接");
            dialogStage.setResizable(false);
            dialogStage.setScene(new Scene(root));

            dialogStage.showAndWait();

            // 刷新连接列表
            loadSavedConnections();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteConnection() {
        TreeItem<String> selectedItem = connectionTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            KafkaConnection connection = findConnectionByTreeItem(selectedItem);
            if (connection != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("确认删除");
                alert.setHeaderText(null);
                alert.setContentText("确定要删除连接 " + connection.getName() + " 吗？");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    ConnectionService.deleteConnection(connection);
                    loadSavedConnections();
                }
            }
        }
    }

    private KafkaConnection findConnectionByTreeItem(TreeItem<String> item) {
        String connectionStr = item.getValue();
        List<KafkaConnection> connections = ConnectionService.loadConnections();
        for (KafkaConnection conn : connections) {
            if (conn.toString().equals(connectionStr)) {
                return conn;
            }
        }
        return null;
    }

    private void handleConnectionSelection(TreeItem<String> connectionItem) {
        // 如果之前展开的是另一个连接，则收起它
        if (lastExpandedItem != null && lastExpandedItem != connectionItem) {
            lastExpandedItem.setExpanded(false);
            lastExpandedItem.getChildren().clear();  // 清空子节点，以便重新加载
        }

        // 如果当前节点还没有加载过主题
        if (connectionItem.getChildren().isEmpty()) {
            // 从连接字符串中解析出连接信息
            String connectionStr = connectionItem.getValue();
            List<KafkaConnection> connections = ConnectionService.loadConnections();
            for (KafkaConnection conn : connections) {
                if (conn.toString().equals(connectionStr)) {
                    loadTopicsForConnection(conn, connectionItem);
                    break;
                }
            }
        }

        // 记住当前展开的连接
        lastExpandedItem = connectionItem;
    }

    private void loadSavedConnections() {
        List<KafkaConnection> connections = ConnectionService.loadConnections();
        TreeItem<String> root = new TreeItem<>();
        connectionTreeView.setShowRoot(false);

        // 为每个连接创建节点
        for (KafkaConnection connection : connections) {
            TreeItem<String> connectionItem = new TreeItem<>(connection.toString());
            connectionItem.setExpanded(false);  // 默认收起
            root.getChildren().add(connectionItem);
        }

        connectionTreeView.setRoot(root);
    }

    private void loadTopicsForConnection(KafkaConnection connection, TreeItem<String> connectionItem) {
        // 获取主题和分区信息
        Map<String, List<TopicPartitionInfo>> topicsWithPartitions =
                KafkaService.getTopicsWithPartitions(connection.getHost(), connection.getPort());

        // 为每个主题创建节点
        for (Map.Entry<String, List<TopicPartitionInfo>> entry : topicsWithPartitions.entrySet()) {
            TreeItem<String> topicItem = new TreeItem<>(entry.getKey());

            // 为每个分区创建子节点
            for (TopicPartitionInfo partition : entry.getValue()) {
                TreeItem<String> partitionItem = new TreeItem<>(
                        "Partition-" + partition.partition() +
                                " (Leader: " + partition.leader().id() + ")"
                );
                topicItem.getChildren().add(partitionItem);
            }

            connectionItem.getChildren().add(topicItem);
        }
    }

    @FXML
    private void handleAddConnection(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/connection_dialog.fxml"));
            Parent root = loader.load();

            ConnectionDialogController controller = loader.getController();
            controller.setMainController(this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("添加Kafka连接");
            dialogStage.setResizable(false);
            dialogStage.setScene(new Scene(root));

            dialogStage.showAndWait();

            // 刷新连接列表
            loadSavedConnections();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void refreshConnections() {
        loadSavedConnections();
    }
}