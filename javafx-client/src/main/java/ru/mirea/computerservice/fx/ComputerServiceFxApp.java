package ru.mirea.computerservice.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Приложение JavaFX. Три ключевых понятия (спрашивают на защите):
 *  - Stage — окно операционной системы (здесь primaryStage — главное окно);
 *  - Scene — содержимое окна: дерево элементов интерфейса, которое загружено из FXML;
 *  - Node — любой элемент этого дерева (кнопка, таблица, панель — всё это Node).
 *
 * Метод start() вызывается JavaFX в потоке интерфейса (JavaFX Application Thread).
 */
public class ComputerServiceFxApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ru/mirea/computerservice/fx/MainView.fxml"));
        Parent root = loader.load(); // здесь же создаётся MainController и внедряются поля @FXML

        primaryStage.setTitle("Компьютерный сервис — учёт заявок на ремонт");
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.setMinWidth(1150);
        primaryStage.setMinHeight(640);
        primaryStage.show();
    }
}
