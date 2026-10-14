package ru.mirea.computerservice.service;

import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.EntityNotFoundException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.repository.RepairRequestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Бизнес-логика заявок на ремонт: проверки перед сохранением, правила статусов,
 * поиск/фильтрация/сортировка (Stream API) и статистика.
 */
public class RepairRequestService {

    private static final Set<RequestStatus> FINAL_STATUSES = Set.of(RequestStatus.DONE, RequestStatus.CANCELLED);

    private final RepairRequestRepository repository;
    private final ClientService clientService;

    public RepairRequestService(RepairRequestRepository repository, ClientService clientService) {
        this.repository = repository;
        this.clientService = clientService;
    }

    // ------------------------------------------------------------------ CRUD

    public RepairRequest create(int clientId, String deviceType, String description, Priority priority) {
        return create(clientId, deviceType, description, priority, BigDecimal.ZERO);
    }

    /** Бизнес-правило: заявку можно создать только для существующего клиента и только с корректными данными. */
    public RepairRequest create(int clientId, String deviceType, String description,
                                Priority priority, BigDecimal cost) {
        Client client = clientService.getById(clientId);
        String device = Validation.deviceType(deviceType);
        String text = Validation.description(description);
        Priority validPriority = Validation.required(priority, "Приоритет");
        BigDecimal validCost = Validation.cost(cost);

        RepairRequest request = new RepairRequest(client.getId(), device, text, validPriority);
        request.setCost(validCost);
        request.setClientName(client.getFullName());
        return repository.save(request);
    }

    public RepairRequest getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена"));
    }

    public List<RepairRequest> getAll() {
        return repository.findAll();
    }

    /** Заявки одного клиента — «просмотр связанных данных». */
    public List<RepairRequest> getByClient(int clientId) {
        return repository.findByClientId(clientId);
    }

    public void changeStatus(int id, RequestStatus newStatus) {
        Validation.required(newStatus, "Статус");
        RepairRequest request = getById(id);
        ensureStatusChangeAllowed(request, newStatus);
        applyStatus(request, newStatus);
        repository.update(request);
    }

    public void setCost(int id, BigDecimal cost) {
        BigDecimal validCost = Validation.cost(cost);
        RepairRequest request = getById(id);
        request.setCost(validCost);
        repository.update(request);
    }

    /**
     * Изменение заявки целиком из формы. Все значения проверяются ДО обращения к базе;
     * правило «из DONE/CANCELLED статус менять нельзя» применяется, только если статус реально меняется.
     */
    public void updateDetails(int id, String deviceType, String description,
                              Priority priority, RequestStatus status, BigDecimal cost) {
        RepairRequest existing = getById(id);

        String device = Validation.deviceType(deviceType);
        String text = Validation.description(description);
        Priority validPriority = Validation.required(priority, "Приоритет");
        RequestStatus validStatus = Validation.required(status, "Статус");
        BigDecimal validCost = Validation.cost(cost);

        if (validStatus != existing.getStatus()) {
            ensureStatusChangeAllowed(existing, validStatus);
        }

        existing.setDeviceType(device);
        existing.setProblemDescription(text);
        existing.setPriority(validPriority);
        existing.setCost(validCost);
        applyStatus(existing, validStatus);

        repository.update(existing);
    }

    public void delete(int id) {
        getById(id);
        repository.delete(id);
    }

    private void ensureStatusChangeAllowed(RepairRequest request, RequestStatus newStatus) {
        if (!request.canTransitionTo(newStatus)) {
            throw new BusinessException("Статус: нельзя изменить статус завершённой или отменённой заявки (№"
                    + request.getId() + ")");
        }
    }

    private void applyStatus(RepairRequest request, RequestStatus newStatus) {
        request.setStatus(newStatus);
        if (newStatus == RequestStatus.DONE && request.getCompletedAt() == null) {
            request.setCompletedAt(LocalDateTime.now());
        }
    }

    // --------------------------------------------- поиск, фильтры, сортировка

    public List<RepairRequest> searchByDescription(String keyword) {
        return repository.searchByDescription(keyword);
    }

    public List<RepairRequest> searchByClientName(String namePart) {
        return repository.searchByClientName(namePart);
    }

    /**
     * Единая точка для таблицы заявок: все условия комбинируются (И), а не заменяют друг друга.
     * <ol>
     *   <li>Если задан текст — SQL-поиск по описанию, устройству и ФИО клиента;
     *       иначе берутся все заявки.</li>
     *   <li>Дальше Stream API: фильтры по статусу, приоритету и диапазону дат создания.</li>
     *   <li>В конце — сортировка выбранным Comparator.</li>
     * </ol>
     * Любой из параметров может быть null («не фильтровать»).
     */
    public List<RepairRequest> findFiltered(String keyword, RequestStatus status, Priority priority,
                                            LocalDate from, LocalDate to, RequestSortOrder order) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Период: дата «с» не может быть позже даты «по»");
        }
        List<RepairRequest> base = (keyword == null || keyword.isBlank())
                ? repository.findAll()
                : repository.searchAnyField(keyword.trim());

        RequestSortOrder effectiveOrder = order == null ? RequestSortOrder.BY_ID : order;
        return base.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> priority == null || r.getPriority() == priority)
                .filter(r -> from == null || !r.getCreatedAt().toLocalDate().isBefore(from))
                .filter(r -> to == null || !r.getCreatedAt().toLocalDate().isAfter(to))
                .sorted(effectiveOrder.comparator())
                .collect(Collectors.toList());
    }

    public List<RepairRequest> filterByStatus(RequestStatus status) {
        return findFiltered(null, status, null, null, null, RequestSortOrder.BY_ID);
    }

    public List<RepairRequest> filterByPriority(Priority priority) {
        return findFiltered(null, null, priority, null, null, RequestSortOrder.BY_ID);
    }

    public List<RepairRequest> filterByDateRange(LocalDate from, LocalDate to) {
        return findFiltered(null, null, null, from, to, RequestSortOrder.BY_ID);
    }

    // -------------------------------------------------------------- статистика

    /** Количество заявок по каждому статусу (включая статусы с нулём). */
    public Map<RequestStatus, Long> countByStatus() {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            result.put(status, 0L);
        }
        repository.findAll().forEach(r -> result.merge(r.getStatus(), 1L, Long::sum));
        return result;
    }

    /**
     * Показатели для блока «Статистика» (требование ТЗ — не менее 5). Значения
     * уже отформатированы в строки, порядок вывода сохраняется (LinkedHashMap).
     */
    public Map<String, String> getStatistics() {
        List<RepairRequest> all = repository.findAll();
        int total = all.size();

        long active = all.stream().filter(r -> !FINAL_STATUSES.contains(r.getStatus())).count();
        long done = all.stream().filter(r -> r.getStatus() == RequestStatus.DONE).count();
        long cancelled = all.stream().filter(r -> r.getStatus() == RequestStatus.CANCELLED).count();
        long highPriority = all.stream()
                .filter(r -> r.getPriority() == Priority.HIGH || r.getPriority() == Priority.URGENT).count();
        long lastMonth = all.stream()
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(LocalDate.now().minusDays(30))).count();

        List<BigDecimal> priced = all.stream()
                .map(RepairRequest::getCost)
                .filter(c -> c != null && c.signum() > 0)
                .collect(Collectors.toList());
        BigDecimal revenue = all.stream()
                .filter(r -> r.getStatus() == RequestStatus.DONE && r.getCost() != null)
                .map(RepairRequest::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Всего клиентов", String.valueOf(clientService.getAll().size()));
        stats.put("Всего заявок", String.valueOf(total));
        stats.put("Активных заявок", String.valueOf(active));
        stats.put("Выполнено", String.valueOf(done));
        stats.put("Отменено", String.valueOf(cancelled));
        stats.put("Высокий и срочный приоритет", count(highPriority, total));
        stats.put("Заявок за последние 30 дней", String.valueOf(lastMonth));
        stats.put("Средняя стоимость ремонта", priced.isEmpty() ? "—" : money(
                priced.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(priced.size()), 2, RoundingMode.HALF_UP)));
        stats.put("Минимальная стоимость", priced.isEmpty() ? "—" : money(priced.stream().min(BigDecimal::compareTo).get()));
        stats.put("Максимальная стоимость", priced.isEmpty() ? "—" : money(priced.stream().max(BigDecimal::compareTo).get()));
        stats.put("Выручка по выполненным заявкам", money(revenue));
        return stats;
    }

    private static String count(long part, int total) {
        double percent = total == 0 ? 0 : 100.0 * part / total;
        return String.format(Locale.forLanguageTag("ru"), "%d (%.0f%%)", part, percent);
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.forLanguageTag("ru"), "%,.2f ₽", value);
    }
}
