package ru.mirea.computerservice.service; // Пакет, в котором находится класс

import ru.mirea.computerservice.exception.BusinessException; // Импорт исключения для нарушения бизнес-правил
import ru.mirea.computerservice.exception.EntityNotFoundException; // Импорт исключения "сущность не найдена"
import ru.mirea.computerservice.model.Client; // Импорт модели Client
import ru.mirea.computerservice.model.Priority; // Импорт enum Priority
import ru.mirea.computerservice.model.RepairRequest; // Импорт модели RepairRequest
import ru.mirea.computerservice.model.RequestStatus; // Импорт enum RequestStatus
import ru.mirea.computerservice.repository.RepairRequestRepository; // Импорт интерфейса репозитория

import java.math.BigDecimal; // Импорт BigDecimal для работы с деньгами
import java.math.RoundingMode; // Импорт RoundingMode для округления
import java.time.LocalDate; // Импорт LocalDate для работы с датами
import java.time.LocalDateTime; // Импорт LocalDateTime для работы с датой и временем
import java.util.Comparator; // Импорт Comparator для сортировки
import java.util.LinkedHashMap; // Импорт LinkedHashMap для сохранения порядка вставки
import java.util.List; // Импорт List
import java.util.Map; // Импорт Map для статистики
import java.util.stream.Collectors; // Импорт Collectors для Stream API

public class RepairRequestService { // Класс сервиса — бизнес-логика для заявок

    private final RepairRequestRepository repository; // Поле для хранения репозитория заявок
    private final ClientService clientService; // Поле для хранения сервиса клиентов (для проверки существования клиента)

    public RepairRequestService(RepairRequestRepository repository, ClientService clientService) { // Конструктор с внедрением зависимостей
        this.repository = repository; // Сохраняем репозиторий
        this.clientService = clientService; // Сохраняем сервис клиентов
    }

    public RepairRequest create(int clientId, String deviceType, String description, Priority priority) { // Метод создания заявки
        Client client = clientService.getById(clientId); // EntityNotFoundException, если клиента нет // Проверяем существование клиента
        if (deviceType == null || deviceType.isBlank()) { // Проверка: тип устройства не пустой
            throw new BusinessException("Тип устройства обязателен для заполнения"); // Бросаем BusinessException
        }
        if (description == null || description.isBlank()) { // Проверка: описание не пустое
            throw new BusinessException("Описание неисправности обязательно для заполнения"); // Бросаем BusinessException
        }
        RepairRequest request = new RepairRequest(client.getId(), deviceType, description, priority); // Создаем объект заявки
        request.setClientName(client.getFullName()); // Устанавливаем имя клиента (денормализация для вывода)
        return repository.save(request); // Сохраняем через репозиторий и возвращаем результат
    }

    public RepairRequest getById(int id) { // Метод получения заявки по id
        return repository.findById(id) // Ищем заявку в репозитории
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена")); // Если пусто — бросаем исключение
    }

    public List<RepairRequest> getAll() { // Метод получения всех заявок
        return repository.findAll(); // Делегируем в репозиторий
    }

    public void changeStatus(int id, RequestStatus newStatus) { // Метод изменения статуса заявки
        RepairRequest request = getById(id); // Получаем заявку (или исключение, если нет)
        if (!request.canTransitionTo(newStatus)) { // Проверяем бизнес-правило: можно ли перейти в новый статус
            throw new BusinessException("Нельзя изменить статус завершённой или отменённой заявки"); // Бросаем BusinessException
        }
        request.setStatus(newStatus); // Устанавливаем новый статус
        if (newStatus == RequestStatus.DONE) { // Если новый статус — DONE
            request.setCompletedAt(LocalDateTime.now()); // Устанавливаем дату завершения
        }
        repository.update(request); // Сохраняем изменения в БД
    }

    public void setCost(int id, BigDecimal cost) { // Метод установки стоимости ремонта
        if (cost.compareTo(BigDecimal.ZERO) < 0) { // Проверка: стоимость не отрицательная
            throw new BusinessException("Стоимость ремонта не может быть отрицательной"); // Бросаем BusinessException
        }
        RepairRequest request = getById(id); // Получаем заявку
        request.setCost(cost); // Устанавливаем стоимость
        repository.update(request); // Сохраняем изменения
    }

    public void delete(int id) { // Метод удаления заявки
        getById(id); // Проверяем, что заявка существует
        repository.delete(id); // Делегируем удаление в репозиторий
    }

    // ---- Поиск ----

    public List<RepairRequest> searchByDescription(String keyword) { // Поиск заявок по описанию
        return repository.searchByDescription(keyword); // Делегируем в репозиторий
    }

    public List<RepairRequest> searchByClientName(String namePart) { // Поиск заявок по имени клиента
        return repository.searchByClientName(namePart); // Делегируем в репозиторий
    }

    // ---- Фильтрация (Stream API) ----

    public List<RepairRequest> filterByStatus(RequestStatus status) { // Фильтрация по статусу
        return repository.findAll().stream() // Получаем все заявки и создаем поток
                .filter(r -> r.getStatus() == status) // Оставляем только с нужным статусом
                .collect(Collectors.toList()); // Собираем обратно в список
    }

    public List<RepairRequest> filterByPriority(Priority priority) { // Фильтрация по приоритету
        return repository.findAll().stream() // Получаем все заявки и создаем поток
                .filter(r -> r.getPriority() == priority) // Оставляем только с нужным приоритетом
                .collect(Collectors.toList()); // Собираем обратно в список
    }

    public List<RepairRequest> filterByDateRange(LocalDate from, LocalDate to) { // Фильтрация по диапазону дат
        return repository.findAll().stream() // Получаем все заявки и создаем поток
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(from) // Дата создания не раньше from
                        && !r.getCreatedAt().toLocalDate().isAfter(to)) // И не позже to
                .collect(Collectors.toList()); // Собираем обратно в список
    }

    // ---- Сортировка (Stream API) ----

    public List<RepairRequest> sortByDate() { // Сортировка по дате создания
        return repository.findAll().stream() // Получаем все заявки и создаем поток
                .sorted(Comparator.comparing(RepairRequest::getCreatedAt)) // Сортируем по createdAt
                .collect(Collectors.toList()); // Собираем обратно в список
    }

    public List<RepairRequest> sortByCost() { // Сортировка по стоимости
        return repository.findAll().stream() // Получаем все заявки и создаем поток
                .sorted(Comparator.comparing(RepairRequest::getCost)) // Сортируем по cost
                .collect(Collectors.toList()); // Собираем обратно в список
    }

    // ---- Статистика (минимум 5 показателей) ----

    public Map<String, Object> getStatistics() { // Метод сбора статистики
        List<RepairRequest> all = repository.findAll(); // Получаем все заявки

        Map<String, Object> stats = new LinkedHashMap<>(); // Создаем Map с сохранением порядка вставки
        stats.put("Всего клиентов", clientService.getAll().size()); // Добавляем количество клиентов
        stats.put("Всего заявок", all.size()); // Добавляем количество заявок

        Map<RequestStatus, Long> byStatus = all.stream() // Создаем поток из заявок
                .collect(Collectors.groupingBy(RepairRequest::getStatus, Collectors.counting())); // Группируем по статусу и считаем количество

        stats.put("В работе", byStatus.getOrDefault(RequestStatus.IN_PROGRESS, 0L)); // Добавляем количество "В работе"
        stats.put("Выполнено", byStatus.getOrDefault(RequestStatus.DONE, 0L)); // Добавляем количество "Выполнено"
        stats.put("Отменено", byStatus.getOrDefault(RequestStatus.CANCELLED, 0L)); // Добавляем количество "Отменено"

        BigDecimal totalCost = all.stream() // Создаем поток из заявок
                .map(RepairRequest::getCost) // Извлекаем стоимость каждой заявки
                .filter(c -> c != null) // Убираем null
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Суммируем все стоимости
        long countWithCost = all.stream() // Создаем поток из заявок
                .filter(r -> r.getCost() != null && r.getCost().compareTo(BigDecimal.ZERO) > 0) // Оставляем только заявки с ценой > 0
                .count(); // Считаем количество
        BigDecimal avg = countWithCost == 0 // Если нет заявок с ценой
                ? BigDecimal.ZERO // То средняя = 0
                : totalCost.divide(BigDecimal.valueOf(countWithCost), 2, RoundingMode.HALF_UP); // Иначе делим сумму на количество с округлением
        stats.put("Средняя стоимость ремонта", avg); // Добавляем среднюю стоимость

        return stats; // Возвращаем Map со статистикой
    }
}