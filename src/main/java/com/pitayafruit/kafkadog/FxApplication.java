package com.pitayafruit.kafkadog;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class FxApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/main.fxml")));
        primaryStage.setTitle("Kafka Dog");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/app.png")));
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(new Scene(root, 1200, 650));
        primaryStage.show();
    }


}