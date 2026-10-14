package ru.mirea.computerservice.repository;

import ru.mirea.computerservice.model.Client;

import java.util.List;
import java.util.Optional;

/**
 * Контракт доступа к данным клиентов. Сервисный слой знает только этот интерфейс,
 * а не JDBC — поэтому в тестах его можно заменить объектом в памяти.
 */
public interface ClientRepository {
    Client save(Client client);

    Optional<Client> findById(int id);

    List<Client> findAll();

    void update(Client client);

    void delete(int id);

    boolean existsByPhone(String phone);

    /** Поиск клиента по точному (нормализованному) номеру телефона. */
    Optional<Client> findByPhone(String phone);

    /** Поиск клиента по точному (нормализованному) email. */
    Optional<Client> findByEmail(String email);

    List<Client> searchByName(String namePart);

    /** Есть ли у клиента незавершённые заявки (не DONE и не CANCELLED). */
    boolean hasActiveRequests(int clientId);

    /** Есть ли у клиента хоть какие-то заявки, включая завершённые и отменённые. */
    boolean hasAnyRequests(int clientId);
}
