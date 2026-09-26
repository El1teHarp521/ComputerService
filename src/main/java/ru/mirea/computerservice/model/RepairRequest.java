package ru.mirea.computerservice.model;

// Импорт классов для работы с числами с плавающей точкой (BigDecimal),
// датой и временем (LocalDateTime), форматированием даты (DateTimeFormatter),
// сравнением объектов (Objects) и множествами (Set).
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

// Класс RepairRequest представляет собой модель (сущность) заявки на ремонт.
public class RepairRequest {

    // Статическая константа для форматирования даты в вид "дд.ММ.гггг чч:мм".
    // static означает, что она принадлежит классу, а не конкретному объекту.
    // final означает, что значение нельзя изменить после инициализации.
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Статическая константа - множество финальных статусов.
    // Set.of(...) создает неизменяемое множество из переданных элементов.
    // Используется для проверки бизнес-правила: из этих статусов нельзя перейти в другой.
    private static final Set<RequestStatus> FINAL_STATUSES =
            Set.of(RequestStatus.DONE, RequestStatus.CANCELLED);

    // Приватные поля класса (инкапсуляция).
    private int id; // Уникальный идентификатор заявки
    private int clientId; // ID клиента, которому принадлежит заявка
    private String clientName; // Имя клиента. Денормализованное поле, заполняется после JOIN-запроса к БД.
    private String deviceType; // Тип устройства (например, "Ноутбук")
    private String problemDescription; // Описание проблемы
    private RequestStatus status; // Текущий статус заявки (enum)
    private Priority priority; // Приоритет заявки (enum)
    private BigDecimal cost; // Стоимость ремонта. BigDecimal используется для денег, чтобы избежать ошибок округления.
    private LocalDateTime createdAt; // Дата и время создания заявки
    private LocalDateTime completedAt; // Дата и время завершения заявки (может быть null)

    // Пустой конструктор. Нужен для создания объекта без параметров (например, при чтении из БД).
    public RepairRequest() {
    }

    // Конструктор с параметрами. Используется для создания новой заявки.
    public RepairRequest(int clientId, String deviceType, String problemDescription, Priority priority) {
        this.clientId = clientId;
        this.deviceType = deviceType;
        this.problemDescription = problemDescription;
        this.priority = priority;
        this.status = RequestStatus.NEW; // Новая заявка всегда получает статус NEW
        this.cost = BigDecimal.ZERO; // Начальная стоимость равна нулю
    }

    /** Бизнес-правило: из финального статуса (DONE/CANCELLED) переход запрещён. */
    // Метод проверяет, можно ли перевести заявку в новый статус.
    public boolean canTransitionTo(RequestStatus newStatus) {
        // Возвращает true, если текущий статус НЕ содержится в множестве финальных статусов.
        // То есть, если заявка не выполнена и не отменена, переход разрешен.
        return !FINAL_STATUSES.contains(this.status);
    }

    // Геттеры и сеттеры для доступа к приватным полям.
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

    // Переопределяем метод equals. Два объекта RepairRequest считаются равными, если у них одинаковый id.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Проверка на ссылочное равенство (один и тот же объект)
        if (!(o instanceof RepairRequest)) return false; // Проверка, что объект является RepairRequest
        RepairRequest that = (RepairRequest) o; // Приведение типа
        return id == that.id; // Сравнение по id
    }

    // Переопределяем hashCode. Если equals сравнивает по id, то hashCode тоже должен использовать id.
    @Override
    public int hashCode() {
        return Objects.hash(id); // Генерация хэш-кода на основе id
    }

    // Переопределяем toString для красивого вывода информации о заявке.
    @Override
    public String toString() {
        // Если createdAt не null, форматируем дату, иначе выводим "-".
        String dateStr = (createdAt != null) ? createdAt.format(DATE_FORMATTER) : "-";
        // Если clientName не null, используем его, иначе выводим "ID: " + clientId.
        String client = (clientName != null) ? clientName : ("ID: " + clientId);

        // String.format создает строку с заданным форматом.
        // %d - целое число, %s - строка, %-10s - строка, выровненная по левому краю, шириной 10 символов.
        return String.format("[%d] от %s | %-10s | Клиент: %-20s | Поломка: %-25s | Статус: %-15s | Приоритет: %-8s | Цена: %s руб.",
                id,
                dateStr,
                deviceType,
                client,
                problemDescription,
                status.getDisplayName(),
                priority.getDisplayName(),
                cost);
    }
}