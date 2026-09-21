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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepairRequestService {

    private final RepairRequestRepository repository;
    private final ClientService clientService;

    public RepairRequestService(RepairRequestRepository repository, ClientService clientService) {
        this.repository = repository;
        this.clientService = clientService;
    }

    public RepairRequest create(int clientId, String deviceType, String description, Priority priority) {
        Client client = clientService.getById(clientId); // EntityNotFoundException, если клиента нет
        if (deviceType == null || deviceType.isBlank()) {
            throw new BusinessException("Тип устройства обязателен для заполнения");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("Описание неисправности обязательно для заполнения");
        }
        RepairRequest request = new RepairRequest(client.getId(), deviceType, description, priority);
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

    public void changeStatus(int id, RequestStatus newStatus) {
        RepairRequest request = getById(id);
        if (!request.canTransitionTo(newStatus)) {
            throw new BusinessException("Нельзя изменить статус завершённой или отменённой заявки");
        }
        request.setStatus(newStatus);
        if (newStatus == RequestStatus.DONE) {
            request.setCompletedAt(LocalDateTime.now());
        }
        repository.update(request);
    }

    public void setCost(int id, BigDecimal cost) {
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Стоимость ремонта не может быть отрицательной");
        }
        RepairRequest request = getById(id);
        request.setCost(cost);
        repository.update(request);
    }

    public void delete(int id) {
        getById(id);
        repository.delete(id);
    }

    // ---- Поиск ----

    public List<RepairRequest> searchByDescription(String keyword) {
        return repository.searchByDescription(keyword);
    }

    public List<RepairRequest> searchByClientName(String namePart) {
        return repository.searchByClientName(namePart);
    }

    // ---- Фильтрация (Stream API) ----

    public List<RepairRequest> filterByStatus(RequestStatus status) {
        return repository.findAll().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<RepairRequest> filterByPriority(Priority priority) {
        return repository.findAll().stream()
                .filter(r -> r.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public List<RepairRequest> filterByDateRange(LocalDate from, LocalDate to) {
        return repository.findAll().stream()
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(from)
                        && !r.getCreatedAt().toLocalDate().isAfter(to))
                .collect(Collectors.toList());
    }

    // ---- Сортировка (Stream API) ----

    public List<RepairRequest> sortByDate() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(RepairRequest::getCreatedAt))
                .collect(Collectors.toList());
    }

    public List<RepairRequest> sortByCost() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(RepairRequest::getCost))
                .collect(Collectors.toList());
    }

    // ---- Статистика (минимум 5 показателей) ----

    public Map<String, Object> getStatistics() {
        List<RepairRequest> all = repository.findAll();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("Всего клиентов", clientService.getAll().size());
        stats.put("Всего заявок", all.size());

        Map<RequestStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(RepairRequest::getStatus, Collectors.counting()));

        stats.put("В работе", byStatus.getOrDefault(RequestStatus.IN_PROGRESS, 0L));
        stats.put("Выполнено", byStatus.getOrDefault(RequestStatus.DONE, 0L));
        stats.put("Отменено", byStatus.getOrDefault(RequestStatus.CANCELLED, 0L));

        BigDecimal totalCost = all.stream()
                .map(RepairRequest::getCost)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long countWithCost = all.stream()
                .filter(r -> r.getCost() != null && r.getCost().compareTo(BigDecimal.ZERO) > 0)
                .count();
        BigDecimal avg = countWithCost == 0
                ? BigDecimal.ZERO
                : totalCost.divide(BigDecimal.valueOf(countWithCost), 2, RoundingMode.HALF_UP);
        stats.put("Средняя стоимость ремонта", avg);

        return stats;
    }
}
