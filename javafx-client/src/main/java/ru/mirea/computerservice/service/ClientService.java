package ru.mirea.computerservice.service;

import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.EntityNotFoundException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.repository.ClientRepository;
import ru.mirea.computerservice.util.EmailSender;

import java.util.List;

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

        Client savedClient = clientRepository.save(new Client(name, normalizedPhone, normalizedEmail));

        if (normalizedEmail != null) {
            String confirmUrl = EmailVerificationServer.createConfirmationUrl(normalizedEmail);
            Thread emailThread = new Thread(() -> {
                try {
                    EmailSender.sendConfirmationEmail(normalizedEmail, name, confirmUrl);
                } catch (Exception e) {
                    System.err.println("Не удалось отправить письмо: " + e.getMessage());
                }
            });
            emailThread.setDaemon(true);
            emailThread.start();
        }

        return savedClient;
    }

    public Client getById(int id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден"));
    }

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

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

    public void resendConfirmationEmail(int clientId) {
        Client client = getById(clientId);

        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw new BusinessException("У клиента «" + client.getFullName() + "» не указан email.");
        }
        if (client.isEmailConfirmed()) {
            throw new BusinessException("У клиента «" + client.getFullName() + "» почта уже подтверждена!");
        }

        String confirmUrl = EmailVerificationServer.createConfirmationUrl(client.getEmail());

        Thread emailThread = new Thread(() -> {
            try {
                EmailSender.sendConfirmationEmail(client.getEmail(), client.getFullName(), confirmUrl);
                System.out.println("Повторное письмо успешно отправлено на: " + client.getEmail());
            } catch (Exception e) {
                System.err.println("Ошибка при повторной отправке письма: " + e.getMessage());
            }
        });
        emailThread.setDaemon(true);
        emailThread.start();
    }

    public void delete(int id) {
        Client client = getById(id);
        if (clientRepository.hasActiveRequests(id)) {
            throw new BusinessException("Нельзя удалить клиента «" + client.getFullName()
                    + "»: у него есть незавершённые заявки.");
        }
        if (clientRepository.hasAnyRequests(id)) {
            throw new BusinessException("Нельзя удалить клиента «" + client.getFullName()
                    + "»: в системе сохранена история его заявок.");
        }
        clientRepository.delete(id);
    }

    public List<Client> searchByName(String query) {
        return clientRepository.searchByName(query);
    }

    private void ensurePhoneFree(String phone, Integer ownId) {
        clientRepository.findByPhone(phone)
                .filter(other -> ownId == null || other.getId() != ownId)
                .ifPresent(other -> {
                    throw new BusinessException("Телефон: номер " + phone
                            + " уже принадлежит клиенту «" + other.getFullName() + "»");
                });
    }

    private void ensureEmailFree(String email, Integer ownId) {
        if (email == null) return;
        clientRepository.findByEmail(email)
                .filter(other -> ownId == null || other.getId() != ownId)
                .ifPresent(other -> {
                    throw new BusinessException("Email: адрес " + email
                            + " уже принадлежит клиенту «" + other.getFullName() + "»");
                });
    }
}