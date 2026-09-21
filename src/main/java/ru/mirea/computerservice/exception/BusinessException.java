package ru.mirea.computerservice.exception;

/**
 * Нарушение бизнес-правила предметной области
 * (например: отрицательная стоимость, запрещённый переход статуса).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
