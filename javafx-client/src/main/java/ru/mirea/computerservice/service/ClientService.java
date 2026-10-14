package ru.mirea.computerservice.service;

import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.EntityNotFoundException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.repository.ClientRepository;

import java.util.List;

/**
 * Бизнес-логика клиентов. Контроллеры JavaFX вызывают ТОЛЬКО этот класс —
 * проверки здесь, а не в обработчиках кнопок.
 */
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client create(String fullName, String phone, String email) {
        String name = Validation.personName(fullName);
        String normalizedPhone = Validation.phone(phone);
        String normalizedEmail = Validation.emailOrNull(email);

        ensurePhoneFree(normalizedPhone, null);
        ensureEmailFree(normalizedEmail, null);

        return clientRepository.save(new Client(name, normalizedPhone, normalizedEmail));
    }

    public Client getById(int id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден"));
    }

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    /**
     * Принимает «сырые» значения из формы, а не готовый объект Client: сначала всё проверяется,
     * и только потом изменяется сохранённая запись. Так объект, который отображается в таблице,
     * не портится, если пользователь ввёл некорректные данные.
     */
    public Client update(int id, String fullName, String phone, String email) {
        Client stored = getById(id);

        String name = Validation.personName(fullName);
        String normalizedPhone = Validation.phone(phone);
        String normalizedEmail = Validation.emailOrNull(email);

        ensurePhoneFree(normalizedPhone, id);
        ensureEmailFree(normalizedEmail, id);

        stored.setFullName(name);
        stored.setPhone(normalizedPhone);
        stored.setEmail(normalizedEmail);
        clientRepository.update(stored);
        return stored;
    }

    public void delete(int id) {
        Client client = getById(id);
        if (clientRepository.hasActiveRequests(id)) {
            throw new BusinessException("Нельзя удалить клиента «" + client.getFullName()
                    + "»: у него есть незавершённые заявки. Сначала завершите или отмените их.");
        }
        if (clientRepository.hasAnyRequests(id)) {
            throw new BusinessException("Нельзя удалить клиента «" + client.getFullName()
                    + "»: в системе сохранена история его заявок (завершённых и отменённых).");
        }
        clientRepository.delete(id);
    }

    public List<Client> searchByName(String query) {
        return clientRepository.searchByName(query);
    }

    // ownId — id редактируемого клиента: его собственный телефон/email не считается «занятым»

    private void ensurePhoneFree(String phone, Integer ownId) {
        clientRepository.findByPhone(phone)
                .filter(other -> ownId == null || other.getId() != ownId)
                .ifPresent(other -> {
                    throw new BusinessException("Телефон: номер " + phone
                            + " уже принадлежит клиенту «" + other.getFullName() + "»");
                });
    }

    private void ensureEmailFree(String email, Integer ownId) {
        if (email == null) {
            return;
        }
        clientRepository.findByEmail(email)
                .filter(other -> ownId == null || other.getId() != ownId)
                .ifPresent(other -> {
                    throw new BusinessException("Email: адрес " + email
                            + " уже принадлежит клиенту «" + other.getFullName() + "»");
                });
    }
}
