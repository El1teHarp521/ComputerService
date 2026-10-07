package ru.mirea.computerservice.repository;

import ru.mirea.computerservice.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {
    Client save(Client client);

    Optional<Client> findById(int id);

    List<Client> findAll();

    void update(Client client);

    void delete(int id);

    boolean existsByPhone(String phone);

    List<Client> searchByName(String namePart);

    boolean hasActiveRequests(int clientId);
}
