package com.pitayafruit.kafkadog.controller;

import com.pitayafruit.kafkadog.model.KafkaConnection;
import com.pitayafruit.kafkadog.service.ConnectionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.CommonClientConfigs;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka连接配置对话框控制器
 * 负责处理连接配置的添加和编辑界面的交互逻辑
 */
public class ConnectionDialogController {

    @FXML private TextField nameField;    // 连接名称输入框
    @FXML private TextField hostField;    // 主机地址输入框
    @FXML private TextField portField;    // 端口号输入框
    @FXML private Button testButton;      // 测试连接按钮
    @FXML private Button saveButton;      // 保存按钮
    @FXML private Button cancelButton;    // 取消按钮

    private MainController mainController;       // 主控制器引用
    private KafkaConnection existingConnection;   // 当前编辑的已存在连接

    /**
     * 设置主控制器引用
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * 测试Kafka连接
     * 异步执行连接测试，避免阻塞UI线程
     */
    @FXML
    private void testConnection() {
        String host = hostField.getText();
        String portText = portField.getText();

        if (host.isEmpty() || portText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请填写完整的连接信息");
            return;
        }

        // 禁用所有控件
        setControlsDisabled(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                int port = Integer.parseInt(portText);
                Properties props = new Properties();
                props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);
                props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "3000");
                props.put(CommonClientConfigs.DEFAULT_API_TIMEOUT_MS_CONFIG, "3000");

                try (AdminClient adminClient = AdminClient.create(props)) {
                    adminClient.listTopics().names().get(3, TimeUnit.SECONDS);
                    return true;
                }
            } catch (Exception e) {
                return e;
            }
        }).thenAcceptAsync(result -> {
            // 重新启用控件
            setControlsDisabled(false);

            if (result instanceof Exception) {
                String errorMessage = result instanceof NumberFormatException ?
                        "端口号格式不正确" : "连接超时，请检查主机地址和端口";
                showAlert(Alert.AlertType.ERROR, "错误", errorMessage);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "成功", "连接测试成功！");
            }
        }, Platform::runLater);
    }

    public void setConnectionForEdit(KafkaConnection connection) {
        this.existingConnection = connection;
        // 填充表单
        nameField.setText(connection.getName());
        hostField.setText(connection.getHost());
        portField.setText(String.valueOf(connection.getPort()));
    }


    /**
     * 保存连接配置
     */
    @FXML
    private void saveConnection() {
        try {
            String name = nameField.getText().trim();  // 添加trim()去除首尾空格
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();

            // 验证输入
            if (name.isEmpty() || host.isEmpty() || portText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "错误", "请填写完整的连接信息");
                return;
            }

            // 验证端口号范围
            int port = Integer.parseInt(portText);
            if (port <= 0 || port > 65535) {
                showAlert(Alert.AlertType.ERROR, "错误", "端口号必须在1-65535之间");
                return;
            }

            KafkaConnection connection;
            if (existingConnection != null) {
                // 编辑现有连接
                existingConnection.setName(name);
                existingConnection.setHost(host);
                existingConnection.setPort(port);
                connection = existingConnection;
            } else {
                // 创建新连接
                connection = new KafkaConnection(name, host, port);
            }

            // 保存连接
            ConnectionService.saveConnection(connection);

            // 通知主控制器刷新连接列表
            if (mainController != null) {
                mainController.refreshConnections();
            }

            closeDialog();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "端口号必须是有效的数字");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "保存连接失败：" + e.getMessage());
        }
    }

    /**
     * 取消操作
     */
    @FXML
    private void cancel() {
        closeDialog();
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    /**
     * 显示警告对话框
     * @param type 警告类型
     * @param title 标题
     * @param content 内容
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 设置所有控件的禁用状态
     * @param disabled 是否禁用
     */
    private void setControlsDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        hostField.setDisable(disabled);
        portField.setDisable(disabled);
        testButton.setDisable(disabled);
        saveButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }
}