package com.pitayafruit.kafkadog.controller;

import com.pitayafruit.kafkadog.model.KafkaConnection;
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

public class ConnectionDialogController {
    @FXML private TextField nameField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private Button testButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

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

    @FXML
    private void saveConnection() {
        try {
            String name = nameField.getText();
            String host = hostField.getText();
            String portText = portField.getText();

            if (name.isEmpty() || host.isEmpty() || portText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "错误", "请填写完整的连接信息");
                return;
            }

            int port = Integer.parseInt(portText);
            KafkaConnection connection = new KafkaConnection(name, host, port);
            mainController.addConnection(connection);

            closeDialog();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "端口号格式不正确");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "保存连接失败：" + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setControlsDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        hostField.setDisable(disabled);
        portField.setDisable(disabled);
        testButton.setDisable(disabled);
        saveButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }
}