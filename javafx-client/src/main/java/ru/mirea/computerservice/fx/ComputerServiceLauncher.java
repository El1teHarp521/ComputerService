package ru.mirea.computerservice.fx;

import javafx.application.Application;

/**
 * Запасная точка входа для запуска из IDE (кнопка Run) и из jar.
 *
 * Если запускать напрямую класс, наследующий javafx.application.Application, а JavaFX
 * подключён как обычная библиотека (classpath, а не module path), JVM пишет
 * «JavaFX runtime components are missing». Отдельный класс-«запускатель» без наследования
 * от Application обходит эту проверку. Maven-плагин (mvn javafx:run) тоже стартует с него.
 */
public final class ComputerServiceLauncher {

    private ComputerServiceLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(ComputerServiceFxApp.class, args);
    }
}
