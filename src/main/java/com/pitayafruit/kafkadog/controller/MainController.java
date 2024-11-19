package com.pitayafruit.kafkadog.controller;

import com.pitayafruit.kafkadog.model.KafkaConnection;
import com.pitayafruit.kafkadog.service.ConnectionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.KafkaFuture;
import javafx.application.Platform;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import javafx.scene.control.ListView;
import com.pitayafruit.kafkadog.model.KafkaMessage;


public class MainController {
    @FXML
    private ListView<KafkaConnection> connectionListView;
    @FXML
    private TreeView<String> topicListView;
    @FXML
    private ListView<KafkaMessage> messageListView;

    private AdminClient currentAdminClient;

    private KafkaConsumer<String, String> consumer;
    private ObservableList<KafkaMessage> messages = FXCollections.observableArrayList();
    private String currentTopic;
    private int currentPartition = -1;

    private ObservableList<KafkaConnection> connections = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 加载保存的连接
        connections.addAll(ConnectionService.loadConnections());
        connectionListView.setItems(connections);
        setupContextMenu();

        // 添加连接选择事件监听
        connectionListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadTopics(newValue);
                    }
                }
        );

        // 设置消息列表及其样式
        messageListView.setItems(messages);
        // 添加这两行来设置空列表时的提示
        Label placeholderLabel = new Label("该分区暂无消息");
        placeholderLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 14px;");
        messageListView.setPlaceholder(placeholderLabel);
        messageListView.setCellFactory(listView -> new ListCell<KafkaMessage>() {
            @Override
            protected void updateItem(KafkaMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // 设置样式
                    if (item.isConsumed()) {
                        setStyle("-fx-text-fill: #808080;");
                    } else {
                        setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // 添加主题树选择事件
        topicListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getValue() != null) {
                        TreeItem<String> item = newValue;
                        if (item.getParent() != null && !item.getParent().getValue().equals("Topics")) {
                            // 选中了分区项
                            String partitionInfo = item.getValue();
                            if (partitionInfo.startsWith("Partition-")) {
                                currentTopic = item.getParent().getValue();
                                currentPartition = Integer.parseInt(
                                        partitionInfo.substring("Partition-".length()).split(" ")[0]
                                );
                                loadMessages();
                            }
                        }
                    }
                }
        );

        // 获取当前Stage并添加关闭事件处理
        Platform.runLater(() -> {
            Stage stage = (Stage) connectionListView.getScene().getWindow();
            stage.setOnCloseRequest(event -> cleanup());
        });
    }

    private void loadMessages() {
        if (currentTopic == null || currentPartition == -1) return;

        CompletableFuture.runAsync(() -> {
            try {
                initializeConsumer();
                TopicPartition partition = new TopicPartition(currentTopic, currentPartition);
                consumer.assign(Collections.singletonList(partition));

                // 获取分区的结束位置
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(partition));
                long endOffset = endOffsets.get(partition);

                // 获取消费者组的已消费位置
                Map<TopicPartition, OffsetAndMetadata> committedOffsets =
                        consumer.committed(Collections.singleton(partition));
                long committedOffset = committedOffsets.get(partition) != null ?
                        committedOffsets.get(partition).offset() : 0;

                // 从头开始读取
                consumer.seekToBeginning(Collections.singletonList(partition));

                List<KafkaMessage> messageList = new ArrayList<>();
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    if (records.isEmpty()) break;

                    for (ConsumerRecord<String, String> record : records) {
                        boolean isConsumed = record.offset() < committedOffset;
                        KafkaMessage message = new KafkaMessage(
                                record.offset(),
                                record.key(),
                                record.value(),
                                record.timestamp(),
                                isConsumed,
                                String.format("%s-%d", currentTopic, currentPartition)
                        );
                        messageList.add(message);
                    }
                }

                // 按时间戳降序排序
                messageList.sort((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));

                Platform.runLater(() -> {
                    messages.clear();
                    messages.addAll(messageList);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误",
                            "加载消息失败: " + e.getMessage());
                });
            }
        });
    }

    // 添加资源清理方法
    public void cleanup() {
        if (consumer != null) {
            consumer.close();
        }
        if (currentAdminClient != null) {
            currentAdminClient.close();
        }
    }

    private void initializeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
        KafkaConnection connection = connectionListView.getSelectionModel().getSelectedItem();
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                connection.getHost() + ":" + connection.getPort());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-dog-viewer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(props);
    }


    private void loadTopics(KafkaConnection connection) {
        // 关闭之前的连接
        if (currentAdminClient != null) {
            currentAdminClient.close();
        }

        // 创建新的连接
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                connection.getHost() + ":" + connection.getPort());

        currentAdminClient = AdminClient.create(props);

        // 清空现有主题列表
        TreeItem<String> root = new TreeItem<>("Topics");
        topicListView.setRoot(root);
        root.setExpanded(true);

        // 异步加载主题信息
        CompletableFuture.runAsync(() -> {
            try {
                // 获取所有主题
                KafkaFuture<Map<String, TopicDescription>> topicsFuture =
                        currentAdminClient.describeTopics(
                                currentAdminClient.listTopics().names().get()
                        ).all();

                Map<String, TopicDescription> topics = topicsFuture.get();

                // 在 JavaFX 线程中更新 UI
                Platform.runLater(() -> {
                    topics.forEach((topicName, description) -> {
                        TreeItem<String> topicItem = new TreeItem<>(topicName);
                        root.getChildren().add(topicItem);

                        // 添加分区信息
                        description.partitions().forEach(partition -> {
                            String partitionInfo = String.format(
                                    "Partition-%d (Leader: %d, Replicas: %s)",
                                    partition.partition(),
                                    partition.leader().id(),
                                    partition.replicas().stream()
                                            .map(node -> String.valueOf(node.id()))
                                            .collect(Collectors.joining(","))
                            );
                            topicItem.getChildren().add(new TreeItem<>(partitionInfo));
                        });
                    });

                    // 按主题名称排序
                    root.getChildren().sort((a, b) ->
                            a.getValue().compareTo(b.getValue())
                    );
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误",
                            "加载主题失败: " + e.getMessage());
                });
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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
                ConnectionService.deleteConnection(selected);
            }
        }
    }

    public void addConnection(KafkaConnection connection) {
        ConnectionService.saveConnection(connection);

        // 如果是编辑现有连接，先移除旧的
        connections.removeIf(c -> c.getId() != null && c.getId().equals(connection.getId()));
        connections.add(connection);
    }
}