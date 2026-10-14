package ru.mirea.computerservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.EntityNotFoundException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверка бизнес-правил заявок, комбинированных фильтров, сортировки и статистики без БД. */
class RepairRequestServiceTest {

    private FakeClientRepository clients;
    private FakeRepairRequestRepository requests;
    private ClientService clientService;
    private RepairRequestService service;
    private Client ivan;
    private Client petr;

    @BeforeEach
    void setUp() {
        clients = new FakeClientRepository(() -> requests.all());
        requests = new FakeRepairRequestRepository(clients);
        clientService = new ClientService(clients);
        service = new RepairRequestService(requests, clientService);
        ivan = clientService.create("Иванов Иван", "+79001112233", null);
        petr = clientService.create("Петров Пётр", "+79002223344", null);
    }

    private String messageOf(Runnable action) {
        return assertThrows(BusinessException.class, action::run).getMessage();
    }

    private RepairRequest seed(Client client, String device, String text, Priority priority,
                               RequestStatus status, String cost, LocalDateTime createdAt) {
        RepairRequest r = new RepairRequest(client.getId(), device, text, priority);
        r.setStatus(status);
        r.setCost(new BigDecimal(cost));
        r.setCreatedAt(createdAt);
        return requests.save(r);
    }

    // ------------------------------------------------------------ создание

    @Test
    @DisplayName("Создание: новая заявка получает статус NEW и переданную стоимость")
    void create_valid_hasStatusNewAndCost() {
        RepairRequest r = service.create(ivan.getId(), " Ноутбук ", " не включается ", Priority.HIGH, new BigDecimal("1500.5"));
        RepairRequest stored = service.getById(r.getId());
        assertEquals(RequestStatus.NEW, stored.getStatus());
        assertEquals(new BigDecimal("1500.50"), stored.getCost());
        assertEquals("Ноутбук", stored.getDeviceType());
        assertEquals("Иванов Иван", stored.getClientName());
    }

    @Test
    @DisplayName("Создание: несуществующий клиент -> EntityNotFoundException")
    void create_unknownClient_throwsNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> service.create(999, "ПК", "не включается", Priority.LOW));
    }

    @Test
    @DisplayName("Создание: обязательные поля и длины проверяются с указанием поля")
    void create_requiredFieldsAndLengths() {
        assertTrue(messageOf(() -> service.create(ivan.getId(), " ", "x", Priority.LOW)).startsWith("Тип устройства"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", " ", Priority.LOW)).startsWith("Описание"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "x".repeat(51), "x", Priority.LOW)).contains("максимум 50"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", "x".repeat(1001), Priority.LOW)).contains("максимум 1000"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", "x", null)).startsWith("Приоритет"));
    }

    @Test
    @DisplayName("Стоимость: отрицательная, с 3 знаками после запятой, слишком большая и null отклоняются")
    void cost_rules() {
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", "x", Priority.LOW, new BigDecimal("-1"))).contains("отрицательной"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", "x", Priority.LOW, new BigDecimal("10.999"))).contains("двух знаков"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", "x", Priority.LOW, new BigDecimal("100000000"))).contains("99 999 999,99"));
        assertTrue(messageOf(() -> service.create(ivan.getId(), "ПК", "x", Priority.LOW, null)).startsWith("Стоимость"));
        // граничное значение допустимо, лишние нули после запятой не мешают
        service.create(ivan.getId(), "ПК", "x", Priority.LOW, new BigDecimal("99999999.99"));
        service.create(ivan.getId(), "ПК", "x", Priority.LOW, new BigDecimal("10.500"));
    }

    // ------------------------------------------------------------ статусы

    @Test
    @DisplayName("Статус: завершённую заявку нельзя вернуть в работу")
    void changeStatus_fromDone_rejected() {
        RepairRequest r = service.create(ivan.getId(), "ПК", "x", Priority.LOW);
        service.changeStatus(r.getId(), RequestStatus.DONE);
        assertTrue(messageOf(() -> service.changeStatus(r.getId(), RequestStatus.IN_PROGRESS)).startsWith("Статус"));
    }

    @Test
    @DisplayName("Статус: при переходе в DONE проставляется дата завершения")
    void updateDetails_toDone_setsCompletedAt() {
        RepairRequest r = service.create(ivan.getId(), "ПК", "x", Priority.LOW);
        assertNull(service.getById(r.getId()).getCompletedAt());
        service.updateDetails(r.getId(), "ПК", "x", Priority.LOW, RequestStatus.DONE, BigDecimal.TEN);
        assertNotNull(service.getById(r.getId()).getCompletedAt());
    }

    @Test
    @DisplayName("Изменение: у отменённой заявки нельзя сменить статус, но можно поправить описание")
    void updateDetails_finishedRequest_statusLocked() {
        RepairRequest r = service.create(ivan.getId(), "ПК", "x", Priority.LOW);
        service.changeStatus(r.getId(), RequestStatus.CANCELLED);

        assertThrows(BusinessException.class, () ->
                service.updateDetails(r.getId(), "ПК", "x", Priority.LOW, RequestStatus.NEW, BigDecimal.ZERO));
        service.updateDetails(r.getId(), "ПК", "уточнённое описание", Priority.LOW, RequestStatus.CANCELLED, BigDecimal.ZERO);
        assertEquals("уточнённое описание", service.getById(r.getId()).getProblemDescription());
    }

    @Test
    @DisplayName("Изменение: ошибочный ввод не портит сохранённую заявку")
    void updateDetails_invalid_leavesStoredUntouched() {
        RepairRequest r = service.create(ivan.getId(), "ПК", "исходное описание", Priority.LOW);
        assertThrows(BusinessException.class, () ->
                service.updateDetails(r.getId(), "ПК", "новое", Priority.HIGH, RequestStatus.NEW, new BigDecimal("-5")));
        assertEquals("исходное описание", service.getById(r.getId()).getProblemDescription());
        assertEquals(Priority.LOW, service.getById(r.getId()).getPriority());
    }

    // ------------------------------------------------- поиск, фильтры, сортировка

    private void seedSampleData() {
        LocalDateTime now = LocalDateTime.now();
        seed(ivan, "Ноутбук", "Не включается", Priority.HIGH, RequestStatus.NEW, "0", now.minusDays(10));
        seed(ivan, "ПК", "Синий экран", Priority.NORMAL, RequestStatus.IN_PROGRESS, "1500", now.minusDays(5));
        seed(petr, "Ноутбук", "Разбит экран", Priority.URGENT, RequestStatus.IN_PROGRESS, "5200", now.minusDays(3));
        seed(petr, "Принтер", "Не печатает", Priority.LOW, RequestStatus.DONE, "800", now.minusDays(1));
    }

    @Test
    @DisplayName("Поиск: без учёта регистра по описанию, устройству и ФИО клиента")
    void findFiltered_keywordSearchesSeveralFields() {
        seedSampleData();
        assertEquals(2, service.findFiltered("НОУТБУК", null, null, null, null, null).size()); // устройство
        assertEquals(2, service.findFiltered("экран", null, null, null, null, null).size());   // описание
        assertEquals(2, service.findFiltered("иванов", null, null, null, null, null).size());  // клиент
        assertEquals(4, service.findFiltered("  ", null, null, null, null, null).size());      // пусто = все
    }

    @Test
    @DisplayName("Фильтры комбинируются (И), а не заменяют друг друга")
    void findFiltered_combinesAllCriteria() {
        seedSampleData();
        assertEquals(1, service.findFiltered("ноутбук", RequestStatus.IN_PROGRESS, null, null, null, null).size());
        assertEquals(1, service.findFiltered(null, RequestStatus.IN_PROGRESS, Priority.URGENT, null, null, null).size());
        assertEquals(0, service.findFiltered("принтер", RequestStatus.NEW, null, null, null, null).size());
    }

    @Test
    @DisplayName("Период дат: границы включаются; перевёрнутый период — ошибка")
    void findFiltered_dateRange() {
        seedSampleData();
        LocalDate today = LocalDate.now();
        assertEquals(2, service.findFiltered(null, null, null, today.minusDays(3), today, null).size());
        assertEquals(1, service.findFiltered(null, null, null, today.minusDays(10), today.minusDays(10), null).size());
        assertTrue(messageOf(() -> service.findFiltered(null, null, null, today, today.minusDays(1), null))
                .startsWith("Период"));
    }

    @Test
    @DisplayName("Сортировка: по стоимости и по приоритету, при равенстве — по номеру")
    void findFiltered_sorting() {
        seedSampleData();
        List<RepairRequest> byCost = service.findFiltered(null, null, null, null, null, RequestSortOrder.COST_DESC);
        assertEquals(new BigDecimal("5200"), byCost.get(0).getCost());
        assertEquals(new BigDecimal("0"), byCost.get(3).getCost());

        List<RepairRequest> byPriority = service.findFiltered(null, null, null, null, null, RequestSortOrder.PRIORITY_DESC);
        assertEquals(Priority.URGENT, byPriority.get(0).getPriority());
        assertEquals(Priority.LOW, byPriority.get(3).getPriority());

        List<RepairRequest> newest = service.findFiltered(null, null, null, null, null, RequestSortOrder.NEWEST_FIRST);
        assertEquals("Принтер", newest.get(0).getDeviceType());
    }

    @Test
    @DisplayName("Связанные данные: заявки одного клиента")
    void getByClient_returnsOnlyOwnRequests() {
        seedSampleData();
        assertEquals(2, service.getByClient(ivan.getId()).size());
        assertEquals(2, service.getByClient(petr.getId()).size());
    }

    // ------------------------------------------------------------ статистика

    @Test
    @DisplayName("Статистика: показатели считаются верно (не менее 5)")
    void statistics_values() {
        seedSampleData();
        Map<String, String> s = service.getStatistics();
        assertTrue(s.size() >= 5);
        assertEquals("2", s.get("Всего клиентов"));
        assertEquals("4", s.get("Всего заявок"));
        assertEquals("3", s.get("Активных заявок"));
        assertEquals("1", s.get("Выполнено"));
        assertTrue(s.get("Высокий и срочный приоритет").startsWith("2 (50%)"));
    }

    @Test
    @DisplayName("Статистика по статусам содержит все статусы, в том числе с нулём")
    void countByStatus_includesZeroes() {
        seedSampleData();
        Map<RequestStatus, Long> counts = service.countByStatus();
        assertEquals(RequestStatus.values().length, counts.size());
        assertEquals(2L, counts.get(RequestStatus.IN_PROGRESS));
        assertEquals(0L, counts.get(RequestStatus.CANCELLED));
    }

    @Test
    @DisplayName("Статистика на пустых данных не падает")
    void statistics_emptyData() {
        Map<String, String> s = service.getStatistics();
        assertEquals("0", s.get("Всего заявок"));
        assertEquals("—", s.get("Средняя стоимость ремонта"));
    }
}
