package ru.mirea.computerservice.model;

// Enum (перечисление) Priority. Используется для хранения фиксированного набора приоритетов.
public enum Priority {
    // Константы перечисления с их "отображаемыми" названиями.
    LOW("Низкий"),
    NORMAL("Обычный"),
    HIGH("Высокий"),
    URGENT("Срочный");

    private final String displayName; // Поле для хранения отображаемого имени

    // Приватный конструктор. Вызывается автоматически при создании констант.
    Priority(String displayName) {
        this.displayName = displayName;
    }

    // Геттер для получения отображаемого имени.
    public String getDisplayName() {
        return displayName;
    }
}