package ru.mirea.computerservice.service;

import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.repository.RepairRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Реализация RepairRequestRepository «в памяти» для тестов сервисного слоя. */
class FakeRepairRequestRepository implements RepairRequestRepository {

    private final Map<Integer, RepairRequest> table = new LinkedHashMap<>();
    private final FakeClientRepository clients;
    private int nextId = 1;

    FakeRepairRequestRepository(FakeClientRepository clients) {
        this.clients = clients;
    }

    Collection<RepairRequest> all() {
        return table.values();
    }

    @Override
    public RepairRequest save(RepairRequest request) {
        request.setId(nextId++);
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }
        table.put(request.getId(), copy(request));
        return request;
    }

    @Override
    public Optional<RepairRequest> findById(int id) {
        return Optional.ofNullable(table.get(id)).map(this::copy);
    }

    @Override
    public List<RepairRequest> findAll() {
        return select(r -> true);
    }

    @Override
    public void update(RepairRequest request) {
        table.put(request.getId(), copy(request));
    }

    @Override
    public void delete(int id) {
        table.remove(id);
    }

    @Override
    public List<RepairRequest> searchByDescription(String keyword) {
        return select(r -> contains(r.getProblemDescription(), keyword));
    }

    @Override
    public List<RepairRequest> searchByClientName(String namePart) {
        return select(r -> contains(clients.nameOf(r.getClientId()), namePart));
    }

    @Override
    public List<RepairRequest> searchAnyField(String keyword) {
        return select(r -> contains(r.getProblemDescription(), keyword)
                || contains(r.getDeviceType(), keyword)
                || contains(clients.nameOf(r.getClientId()), keyword));
    }

    @Override
    public List<RepairRequest> findByClientId(int clientId) {
        return select(r -> r.getClientId() == clientId);
    }

    private List<RepairRequest> select(Predicate<RepairRequest> filter) {
        List<RepairRequest> result = new ArrayList<>();
        table.values().stream().filter(filter).forEach(r -> result.add(copy(r)));
        return result;
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private RepairRequest copy(RepairRequest s) {
        RepairRequest r = new RepairRequest(s.getClientId(), s.getDeviceType(), s.getProblemDescription(), s.getPriority());
        r.setId(s.getId());
        r.setStatus(s.getStatus());
        r.setCost(s.getCost());
        r.setCreatedAt(s.getCreatedAt());
        r.setCompletedAt(s.getCompletedAt());
        r.setClientName(clients.nameOf(s.getClientId())); // как JOIN с таблицей clients в настоящем репозитории
        return r;
    }
}
