package ru.mirea.computerservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public class RepairRequest {

    private static final Set<RequestStatus> FINAL_STATUSES =
            Set.of(RequestStatus.DONE, RequestStatus.CANCELLED);

    private int id;
    private int clientId;
    private String clientName; // денормализовано после JOIN, для удобного вывода
    private String deviceType;
    private String problemDescription;
    private RequestStatus status;
    private Priority priority;
    private BigDecimal cost;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public RepairRequest() {
    }

    public RepairRequest(int clientId, String deviceType, String problemDescription, Priority priority) {
        this.clientId = clientId;
        this.deviceType = deviceType;
        this.problemDescription = problemDescription;
        this.priority = priority;
        this.status = RequestStatus.NEW;
        this.cost = BigDecimal.ZERO;
    }

    /** Бизнес-правило: из финального статуса (DONE/CANCELLED) переход запрещён. */
    public boolean canTransitionTo(RequestStatus newStatus) {
        return !FINAL_STATUSES.contains(this.status);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepairRequest)) return false;
        RepairRequest that = (RepairRequest) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%d] %-12s | клиент: %-22s | статус: %-20s | приоритет: %-9s | стоимость: %s",
                id, deviceType, clientName == null ? String.valueOf(clientId) : clientName,
                status.getDisplayName(), priority.getDisplayName(), cost);
    }
}
