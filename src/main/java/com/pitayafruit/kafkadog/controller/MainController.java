package com.pitayafruit.kafkadog.controller;


import com.pitayafruit.kafkadog.model.KafkaConnection;
import com.pitayafruit.kafkadog.model.KafkaMessage;
import com.pitayafruit.kafkadog.service.ConnectionService;
import com.pitayafruit.kafkadog.service.KafkaMessageService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.pitayafruit.kafkadog.service.KafkaService;
import javafx.scene.control.*;
import org.apache.kafka.common.TopicPartitionInfo;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;


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
    private TableView<KafkaMessage> messageTable;
    @FXML
    private ComboBox<String> offsetComboBox;

    @FXML
    private TableColumn<KafkaMessage, Long> offsetColumn;
    @FXML
    private TableColumn<KafkaMessage, String> timestampColumn;
    @FXML
    private TableColumn<KafkaMessage, String> keyColumn;
    @FXML
    private TableColumn<KafkaMessage, String> valueColumn;
    @FXML
    private TableColumn<KafkaMessage, String> consumedColumn;

    private KafkaConnection currentConnection;
    private String currentTopic;
    private int currentPartition;

    @FXML
    private ComboBox<Integer> pageSizeComboBox;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Label currentPageLabel;

    private int currentPage = 1;
    private long currentOffset = 0;  // 当前页起始offset
    private int pageSize = 20;       // 默认每页20条

    @FXML
    public void initialize() {
        loadSavedConnections();
        setupTreeViewListener();
        setupContextMenu();

        // 初始化offset选项
        offsetComboBox.getItems().addAll("Latest", "Earliest");
        offsetComboBox.setValue("Latest");

        // 初始化表格列
        initializeMessageTable();

        // 添加分区选择监听器
        connectionTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getParent() != null
                            && newValue.getParent().getParent() != null
                            && newValue.getValue().startsWith("Partition-")) {
                        handlePartitionSelection(newValue);
                    }
                });

        // 初始化分页大小选项
        pageSizeComboBox.getItems().addAll(10, 20, 50, 100);
        pageSizeComboBox.setValue(20);
        pageSizeComboBox.setOnAction(e -> {
            pageSize = pageSizeComboBox.getValue();
            currentPage = 1;
            currentOffset = 0;
            loadMessages();
        });
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            // 向前翻页时需要计算新的offset
            currentOffset = Math.max(0, currentOffset - pageSize);
            loadMessages();
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        currentOffset += pageSize;
        loadMessages();
    }


    private void initializeMessageTable() {
        offsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        timestampColumn.setCellValueFactory(cellData -> {
            long timestamp = cellData.getValue().getTimestamp();
            String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(timestamp));
            return new SimpleStringProperty(formattedTime);
        });
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // 保持String类型，但添加样式
        consumedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isConsumed() ? "已消费" : "未消费"));
        consumedColumn.setCellFactory(column -> new TableCell<KafkaMessage, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("status-consumed", "status-unconsumed");
                } else {
                    setText(item);
                    getStyleClass().removeAll("status-consumed", "status-unconsumed");
                    getStyleClass().add(item.equals("已消费") ? "status-consumed" : "status-unconsumed");
                }
            }
        });
    }

    private void handlePartitionSelection(TreeItem<String> partitionItem) {
        // 解析连接信息
        TreeItem<String> topicItem = partitionItem.getParent();
        TreeItem<String> connectionItem = topicItem.getParent();

        // 获取当前连接
        String connectionStr = connectionItem.getValue();
        List<KafkaConnection> connections = ConnectionService.loadConnections();
        for (KafkaConnection conn : connections) {
            if (conn.toString().equals(connectionStr)) {
                currentConnection = conn;
                break;
            }
        }

        currentTopic = topicItem.getValue();
        // 从"Partition-X"中提取分区号
        currentPartition = Integer.parseInt(partitionItem.getValue()
                .split("Partition-")[1].split(" ")[0]);

        // 加载消息
        loadMessages();
        // 重置分页状态
        currentPage = 1;
        currentOffset = 0;
        loadMessages();
    }

    @FXML
    private void handleRefreshMessages() {
        loadMessages();
    }

    private void loadMessages() {
        if (currentConnection == null || currentTopic == null) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                // 修改预览方法的调用，添加分页参数
                return KafkaMessageService.previewMessages(
                        currentConnection.getHost(),
                        currentConnection.getPort(),
                        currentTopic,
                        currentPartition,
                        String.valueOf(currentOffset),  // 使用具体的offset而不是earliest/latest
                        pageSize
                );
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenAcceptAsync(messages -> {
            messageTable.getItems().clear();
            messageTable.getItems().addAll(messages);
            // 更新分页控件状态
            updatePaginationControls(messages);
        }, Platform::runLater).exceptionally(throwable -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("加载消息失败: " + throwable.getMessage());
                alert.show();
            });
            return null;
        });
    }

    private void updatePaginationControls(List<KafkaMessage> messages) {
        currentPageLabel.setText(String.valueOf(currentPage));
        prevButton.setDisable(currentPage == 1);
        // 如果返回的消息数量小于页大小，说明是最后一页
        nextButton.setDisable(messages.size() < pageSize);
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
        connectionTreeView.setOnMouseClicked(event -> {
            TreeItem<String> selectedItem = connectionTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.getParent() == connectionTreeView.getRoot()) {
                    // 连接节点的处理
                    if (event.getButton() == MouseButton.SECONDARY) {
                        // 右键点击显示菜单
                        connectionContextMenu.show(connectionTreeView, event.getScreenX(), event.getScreenY());
                    } else if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        // 双击重新加载连接
                        loadTopicsAsync(findConnectionByTreeItem(selectedItem), selectedItem,
                                (ImageView) selectedItem.getGraphic());
                    }
                } else if (selectedItem.getParent().getParent() == connectionTreeView.getRoot()
                        && event.getButton() == MouseButton.PRIMARY) {
                    // topic节点的处理：单击就展开
                    selectedItem.setExpanded(!selectedItem.isExpanded());
                }
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
                    // 异步加载主题的同时更新图标
                    ImageView loadingIcon = (ImageView) connectionItem.getGraphic();
                    loadTopicsAsync(conn, connectionItem, loadingIcon);
                    break;
                }
            }
        }
        // 记住当前展开的连接
        lastExpandedItem = connectionItem;
    }

    private void loadTopicsAsync(KafkaConnection connection, TreeItem<String> connectionItem,
                                 ImageView iconView) {
        if (connection == null) return;

        // 清空现有节点
        connectionItem.getChildren().clear();

        // 添加加载提示
        TreeItem<String> loadingItem = new TreeItem<>("正在加载...");
        connectionItem.getChildren().add(loadingItem);

        CompletableFuture.supplyAsync(() -> {
            try {
                return KafkaService.getTopicsWithPartitions(connection.getHost(), connection.getPort());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenAcceptAsync(topicsWithPartitions -> {
            // 清空所有子节点(包括加载提示)
            connectionItem.getChildren().clear();

            // 更新连接状态图标为成功
            Platform.runLater(() -> {
                Image successImage = new Image(Objects.requireNonNull(getClass()
                        .getResourceAsStream("/icons/kafka-success.png")));
                iconView.setImage(successImage);
            });

            // 添加主题和分区信息
            for (Map.Entry<String, List<TopicPartitionInfo>> entry : topicsWithPartitions.entrySet()) {
                TreeItem<String> topicItem = new TreeItem<>(entry.getKey());

                for (TopicPartitionInfo partition : entry.getValue()) {
                    TreeItem<String> partitionItem = new TreeItem<>(
                            "Partition-" + partition.partition() +
                                    " (Leader: " + partition.leader().id() + ")"
                    );
                    topicItem.getChildren().add(partitionItem);
                }

                connectionItem.getChildren().add(topicItem);
            }

            // 连接成功后自动展开节点
            Platform.runLater(() -> {
                connectionItem.setExpanded(true);
            });

        }, Platform::runLater).exceptionally(throwable -> {
            Platform.runLater(() -> {
                // 清空所有子节点(包括加载提示)
                connectionItem.getChildren().clear();

                // 更新连接状态图标为失败
                Image failImage = new Image(Objects.requireNonNull(getClass()
                        .getResourceAsStream("/icons/kafka-fail.png")));
                iconView.setImage(failImage);
            });
            return null;
        });
    }


    private void loadSavedConnections() {
        List<KafkaConnection> connections = ConnectionService.loadConnections();
        TreeItem<String> root = new TreeItem<>();
        connectionTreeView.setShowRoot(false);

        // 加载图片
        Image successImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/kafka-success.png")));
        Image failImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/kafka-fail.png")));


        // 为每个连接创建节点
        for (KafkaConnection connection : connections) {
            // 创建带有图标的 TreeItem
            ImageView iconView = new ImageView(failImage); // 默认使用未连接状态的图标
            iconView.setFitHeight(18);
            iconView.setFitWidth(18);

            TreeItem<String> connectionItem = new TreeItem<>(connection.toString(), iconView);

            connectionItem.setExpanded(false);  // 默认收起
            root.getChildren().add(connectionItem);
        }

        connectionTreeView.setRoot(root);
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