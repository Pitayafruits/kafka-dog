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

/**
 * 主界面控制器类
 * 负责管理Kafka连接、主题显示和消息查看
 */
public class MainController {
    // UI组件注入
    @FXML
    private ListView<KafkaConnection> connectionListView; // Kafka连接列表视图
    @FXML
    private TreeView<String> topicListView;              // 主题树形视图
    @FXML
    private ListView<KafkaMessage> messageListView;      // 消息列表视图

    // Kafka客户端实例
    private AdminClient currentAdminClient;              // Kafka管理客户端
    private KafkaConsumer<String, String> consumer;      // Kafka消费者客户端

    // 数据模型
    private ObservableList<KafkaMessage> messages = FXCollections.observableArrayList();  // 消息列表数据
    private String currentTopic;                         // 当前选中的主题
    private int currentPartition = -1;                   // 当前选中的分区
    private ObservableList<KafkaConnection> connections = FXCollections.observableArrayList();  // 连接列表数据

    /**
     * 初始化方法，在FXML加载后自动调用
     */
    @FXML
    public void initialize() {
        // 加载已保存的Kafka连接
        connections.addAll(ConnectionService.loadConnections());
        connectionListView.setItems(connections);
        setupContextMenu();  // 设置右键菜单

        // 监听连接选择事件
        connectionListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadTopics(newValue);  // 加载选中连接的主题列表
                    }
                }
        );

        // 配置消息列表视图
        messageListView.setItems(messages);
        // 设置空列表提示
        Label placeholderLabel = new Label("该分区暂无消息");
        placeholderLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 14px;");
        messageListView.setPlaceholder(placeholderLabel);

        // 自定义消息列表单元格渲染
        messageListView.setCellFactory(listView -> new ListCell<KafkaMessage>() {
            @Override
            protected void updateItem(KafkaMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // 已消费的消息显示灰色，未消费的消息显示黑色加粗
                    if (item.isConsumed()) {
                        setStyle("-fx-text-fill: #808080;");
                    } else {
                        setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // 监听主题树选择事件
        topicListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getValue() != null) {
                        TreeItem<String> item = newValue;
                        if (item.getParent() != null && !item.getParent().getValue().equals("Topics")) {
                            // 当选中分区节点时
                            String partitionInfo = item.getValue();
                            if (partitionInfo.startsWith("Partition-")) {
                                currentTopic = item.getParent().getValue();
                                currentPartition = Integer.parseInt(
                                        partitionInfo.substring("Partition-".length()).split(" ")[0]
                                );
                                loadMessages();  // 加载该分区的消息
                            }
                        }
                    }
                }
        );

        // 注册窗口关闭事件处理
        Platform.runLater(() -> {
            Stage stage = (Stage) connectionListView.getScene().getWindow();
            stage.setOnCloseRequest(event -> cleanup());
        });
    }

    /**
     * 加载指定分区的消息
     */
    private void loadMessages() {
        if (currentTopic == null || currentPartition == -1) return;

        CompletableFuture.runAsync(() -> {
            try {
                initializeConsumer();  // 初始化消费者
                TopicPartition partition = new TopicPartition(currentTopic, currentPartition);
                consumer.assign(Collections.singletonList(partition));

                // 获取分区末尾偏移量
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(partition));
                long endOffset = endOffsets.get(partition);

                // 获取消费者组已提交的偏移量
                Map<TopicPartition, OffsetAndMetadata> committedOffsets =
                        consumer.committed(Collections.singleton(partition));
                long committedOffset = committedOffsets.get(partition) != null ?
                        committedOffsets.get(partition).offset() : 0;

                // 从分区起始位置开始读取
                consumer.seekToBeginning(Collections.singletonList(partition));

                // 收集消息
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

                // 更新UI
                Platform.runLater(() -> {
                    messages.clear();
                    messages.addAll(messageList);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "加载消息失败: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (consumer != null) {
            consumer.close();
        }
        if (currentAdminClient != null) {
            currentAdminClient.close();
        }
    }

    /**
     * 初始化Kafka消费者
     */
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

    /**
     * 加载指定连接的主题列表
     */
    private void loadTopics(KafkaConnection connection) {
        // 关闭现有连接
        if (currentAdminClient != null) {
            currentAdminClient.close();
        }

        // 创建新的管理客户端
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                connection.getHost() + ":" + connection.getPort());

        currentAdminClient = AdminClient.create(props);

        // 初始化主题树
        TreeItem<String> root = new TreeItem<>("Topics");
        topicListView.setRoot(root);
        root.setExpanded(true);

        // 异步加载主题信息
        CompletableFuture.runAsync(() -> {
            try {
                // 获取所有主题及其描述
                KafkaFuture<Map<String, TopicDescription>> topicsFuture =
                        currentAdminClient.describeTopics(
                                currentAdminClient.listTopics().names().get()
                        ).all();

                Map<String, TopicDescription> topics = topicsFuture.get();

                // 在JavaFX线程中更新UI
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

                    // 主题名称排序
                    root.getChildren().sort((a, b) ->
                            a.getValue().compareTo(b.getValue())
                    );
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "加载主题失败: " + e.getMessage());
                });
            }
        });
    }

    // 以下是辅助方法

    /**
     * 显示警告对话框
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 设置连接列表的右键菜单
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("编辑");
        MenuItem deleteItem = new MenuItem("删除");

        editItem.setOnAction(e -> editConnection());
        deleteItem.setOnAction(e -> deleteConnection());

        contextMenu.getItems().addAll(editItem, deleteItem);
        connectionListView.setContextMenu(contextMenu);
    }

    /**
     * 显示添加连接对话框
     */
    @FXML
    private void showAddConnectionDialog() {
        showConnectionDialog(null);
    }

    /**
     * 编辑选中的连接
     */
    private void editConnection() {
        KafkaConnection selected = connectionListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showConnectionDialog(selected);
        }
    }

    /**
     * 显示连接配置对话框
     */
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

    /**
     * 删除选中的连接
     */
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

    /**
     * 添加或更新连接
     */
    public void addConnection(KafkaConnection connection) {
        ConnectionService.saveConnection(connection);

        // 如果是编辑现有连接，先移除旧的
        connections.removeIf(c -> c.getId() != null && c.getId().equals(connection.getId()));
        connections.add(connection);
    }
}