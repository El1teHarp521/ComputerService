package ru.mirea.computerservice.service;

import ru.mirea.computerservice.model.RepairRequest;

import java.util.Comparator;

/**
 * Способы сортировки заявок. У каждого значения есть название для выпадающего списка
 * и готовый Comparator — это пример того, как enum может хранить поведение.
 * Второй критерий (номер заявки) делает порядок стабильным при равных значениях.
 */
public enum RequestSortOrder {
    BY_ID("По номеру заявки",
            Comparator.comparingInt(RepairRequest::getId)),
    NEWEST_FIRST("Сначала новые",
            Comparator.comparing(RepairRequest::getCreatedAt).reversed()
                    .thenComparing(RepairRequest::getId)),
    OLDEST_FIRST("Сначала старые",
            Comparator.comparing(RepairRequest::getCreatedAt)
                    .thenComparing(RepairRequest::getId)),
    COST_DESC("Стоимость: по убыванию",
            Comparator.comparing(RepairRequest::getCost).reversed()
                    .thenComparing(RepairRequest::getId)),
    COST_ASC("Стоимость: по возрастанию",
            Comparator.comparing(RepairRequest::getCost)
                    .thenComparing(RepairRequest::getId)),
    PRIORITY_DESC("Приоритет: срочные первыми",
            Comparator.comparing(RepairRequest::getPriority).reversed()
                    .thenComparing(RepairRequest::getId));

    private final String displayName;
    private final Comparator<RepairRequest> comparator;

    RequestSortOrder(String displayName, Comparator<RepairRequest> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }

    public Comparator<RepairRequest> comparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
