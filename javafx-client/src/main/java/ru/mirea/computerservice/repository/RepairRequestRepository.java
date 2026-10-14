package ru.mirea.computerservice.repository;

import ru.mirea.computerservice.model.RepairRequest;

import java.util.List;
import java.util.Optional;

public interface RepairRequestRepository {
    RepairRequest save(RepairRequest request);

    Optional<RepairRequest> findById(int id);

    List<RepairRequest> findAll();

    void update(RepairRequest request);

    void delete(int id);

    List<RepairRequest> searchByDescription(String keyword);

    List<RepairRequest> searchByClientName(String namePart);

    /** Поиск сразу по нескольким полям: описание неисправности, тип устройства, ФИО клиента. */
    List<RepairRequest> searchAnyField(String keyword);

    /** Все заявки одного клиента — для просмотра связанных данных. */
    List<RepairRequest> findByClientId(int clientId);
}
