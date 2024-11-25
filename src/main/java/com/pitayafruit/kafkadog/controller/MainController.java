package com.pitayafruit.kafkadog.controller;


import com.pitayafruit.kafkadog.model.KafkaConnection;
import com.pitayafruit.kafkadog.service.ConnectionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.pitayafruit.kafkadog.service.KafkaService;
import javafx.scene.control.*;
import org.apache.kafka.common.TopicPartitionInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @FXML
    public void initialize() {
        loadSavedConnections();
        setupTreeViewListener();
    }

    private void setupTreeViewListener() {
        connectionTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getParent() == connectionTreeView.getRoot()) {
                // 如果选中的是连接节点
                handleConnectionSelection(newValue);
            }
        });
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