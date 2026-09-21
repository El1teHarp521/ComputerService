package ru.mirea.computerservice.model;

public enum RequestStatus {
    NEW("Принята"),
    DIAGNOSTICS("Диагностика"),
    IN_PROGRESS("В ремонте"),
    WAITING_PARTS("Ожидание запчастей"),
    DONE("Выполнена"),
    CANCELLED("Отменена");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
