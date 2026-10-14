package ru.mirea.computerservice.fx.util;

import javafx.concurrent.Task;
import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.DatabaseException;
import ru.mirea.computerservice.exception.EntityNotFoundException;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Запуск работы с базой данных и файлами в ФОНОВОМ потоке.
 *
 * Почему это нужно: весь интерфейс JavaFX живёт в одном потоке (JavaFX Application Thread).
 * Если выполнить запрос к PostgreSQL прямо в обработчике кнопки, на время запроса окно
 * «замирает» (а при недоступной БД — на несколько секунд). Поэтому тяжёлая работа выполняется
 * в Task, а результат возвращается обратно в UI-поток через onSuccess / onFailure.
 */
public final class FxTasks {

    private FxTasks() {
    }

    /**
     * @param work      код, выполняемый в фоне (JDBC, экспорт и т.п.); может бросать исключения
     * @param onSuccess вызывается в UI-потоке с результатом
     * @param onFailure вызывается в UI-потоке с исключением
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));

        Thread thread = new Thread(task, "computer-service-worker");
        thread.setDaemon(true); // не мешает закрытию приложения
        thread.start();
    }

    /** Превращает исключение в понятный пользователю текст (без stack trace). */
    public static String friendly(Throwable error) {
        if (error instanceof BusinessException || error instanceof EntityNotFoundException) {
            return error.getMessage();
        }
        if (error instanceof DatabaseException) {
            return error.getMessage()
                    + "\n\nПроверьте, что PostgreSQL запущен, и что параметры подключения в database.properties верны.";
        }
        if (error instanceof ExceptionInInitializerError && error.getMessage() != null) {
            return error.getMessage();
        }
        if (error instanceof java.io.IOException) {
            return "Не удалось записать файл: " + error.getMessage();
        }
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank() ? "Непредвиденная ошибка" : "Непредвиденная ошибка: " + message;
    }
}
