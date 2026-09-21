package ru.mirea.computerservice.exception;

/**
 * Обёртка над SQLException / ошибками подключения к БД,
 * чтобы верхние слои не работали с checked-исключениями напрямую.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
