package ru.mirea.computerservice.exception;

/**
 * Запись с указанным идентификатором не найдена.
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
