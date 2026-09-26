package ru.mirea.computerservice.repository; // Пакет, в котором находится интерфейс

import ru.mirea.computerservice.model.Client; // Импорт модели Client

import java.util.List; // Импорт List для возврата списков
import java.util.Optional; // Импорт Optional для возврата "может быть null"

public interface ClientRepository { // Объявление интерфейса ClientRepository

    Client save(Client client); // Сохранить нового клиента, вернуть его с присвоенным id

    Optional<Client> findById(int id); // Найти клиента по id, вернуть Optional (может быть пусто)

    List<Client> findAll(); // Получить список всех клиентов

    void update(Client client); // Обновить данные существующего клиента

    void delete(int id); // Удалить клиента по id

    boolean existsByPhone(String phone); // Проверить, есть ли клиент с таким телефоном

    List<Client> searchByName(String namePart); // Найти клиентов по части имени

    boolean hasActiveRequests(int clientId); // Проверить, есть ли у клиента активные заявки
}