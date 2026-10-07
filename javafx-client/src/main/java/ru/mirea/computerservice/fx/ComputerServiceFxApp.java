package ru.mirea.computerservice.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Точка входа JavaFX-приложения. Аналог Main.java из консольной КР1,
 * только вместо цикла с Scanner здесь загружается главное окно из FXML.
 */
public class ComputerServiceFxApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ru/mirea/computerservice/fx/MainView.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Компьютерный сервис — учёт заявок на ремонт");
        primaryStage.setScene(new Scene(root, 1100, 650));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
