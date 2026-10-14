package ru.mirea.computerservice.service;

import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.repository.ClientRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Реализация ClientRepository «в памяти» — подменяет JDBC в тестах.
 * Это возможно именно потому, что сервис зависит от ИНТЕРФЕЙСА, а не от JDBC-класса.
 * Хранит копии объектов (как база данных): изменение объекта снаружи не меняет «таблицу».
 */
class FakeClientRepository implements ClientRepository {

    private final Map<Integer, Client> table = new LinkedHashMap<>();
    private final Supplier<Collection<RepairRequest>> requests;
    private int nextId = 1;

    FakeClientRepository(Supplier<Collection<RepairRequest>> requests) {
        this.requests = requests;
    }

    @Override
    public Client save(Client client) {
        client.setId(nextId++);
        client.setRegisteredAt(LocalDateTime.now());
        table.put(client.getId(), copy(client));
        return client;
    }

    @Override
    public Optional<Client> findById(int id) {
        return Optional.ofNullable(table.get(id)).map(FakeClientRepository::copy);
    }

    @Override
    public Optional<Client> findByPhone(String phone) {
        return table.values().stream().filter(c -> phone.equals(c.getPhone())).findFirst().map(FakeClientRepository::copy);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return table.values().stream().filter(c -> email.equals(c.getEmail())).findFirst().map(FakeClientRepository::copy);
    }

    @Override
    public List<Client> findAll() {
        List<Client> result = new ArrayList<>();
        table.values().forEach(c -> result.add(copy(c)));
        return result;
    }

    @Override
    public void update(Client client) {
        table.put(client.getId(), copy(client));
    }

    @Override
    public void delete(int id) {
        table.remove(id);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return findByPhone(phone).isPresent();
    }

    @Override
    public List<Client> searchByName(String namePart) {
        String needle = namePart.toLowerCase(Locale.ROOT);
        List<Client> result = new ArrayList<>();
        table.values().stream()
                .filter(c -> c.getFullName().toLowerCase(Locale.ROOT).contains(needle))
                .forEach(c -> result.add(copy(c)));
        return result;
    }

    @Override
    public boolean hasActiveRequests(int clientId) {
        return requests.get().stream().anyMatch(r -> r.getClientId() == clientId
                && r.getStatus() != RequestStatus.DONE && r.getStatus() != RequestStatus.CANCELLED);
    }

    @Override
    public boolean hasAnyRequests(int clientId) {
        return requests.get().stream().anyMatch(r -> r.getClientId() == clientId);
    }

    String nameOf(int id) {
        Client c = table.get(id);
        return c == null ? null : c.getFullName();
    }

    private static Client copy(Client source) {
        Client c = new Client(source.getFullName(), source.getPhone(), source.getEmail());
        c.setId(source.getId());
        c.setRegisteredAt(source.getRegisteredAt());
        return c;
    }
}
