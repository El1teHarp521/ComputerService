package ru.mirea.computerservice.service;

import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.EntityNotFoundException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.repository.ClientRepository;

import java.util.List;

public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client create(String fullName, String phone, String email) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО клиента обязательно для заполнения");
        }
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Телефон клиента обязателен для заполнения");
        }
        if (clientRepository.existsByPhone(phone)) {
            throw new BusinessException("Клиент с телефоном " + phone + " уже существует");
        }
        Client client = new Client(fullName, phone, email);
        return clientRepository.save(client);
    }

    public Client getById(int id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден"));
    }

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    public void update(Client client) {
        getById(client.getId()); // проверка существования — бросит EntityNotFoundException
        if (client.getFullName() == null || client.getFullName().isBlank()) {
            throw new BusinessException("ФИО клиента обязательно для заполнения");
        }
        clientRepository.update(client);
    }

    public void delete(int id) {
        getById(id);
        if (clientRepository.hasActiveRequests(id)) {
            throw new BusinessException("Нельзя удалить клиента с активными (незавершёнными) заявками");
        }
        clientRepository.delete(id);
    }

    public List<Client> searchByName(String query) {
        return clientRepository.searchByName(query);
    }
}
