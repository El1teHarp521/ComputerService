package ru.mirea.computerservice.repository; // Пакет, в котором находится интерфейс

import ru.mirea.computerservice.model.RepairRequest; // Импорт модели RepairRequest

import java.util.List; // Импорт List для возврата списков
import java.util.Optional; // Импорт Optional для возврата "может быть null"

public interface RepairRequestRepository { // Объявление интерфейса RepairRequestRepository

    RepairRequest save(RepairRequest request); // Сохранить новую заявку, вернуть её с присвоенным id

    Optional<RepairRequest> findById(int id); // Найти заявку по id, вернуть Optional (может быть пусто)

    List<RepairRequest> findAll(); // Получить список всех заявок

    void update(RepairRequest request); // Обновить данные существующей заявки

    void delete(int id); // Удалить заявку по id

    List<RepairRequest> searchByDescription(String keyword); // Найти заявки по ключевому слову в описании

    List<RepairRequest> searchByClientName(String namePart); // Найти заявки по части имени клиента
}