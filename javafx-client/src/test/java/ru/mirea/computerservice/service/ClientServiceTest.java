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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверка бизнес-правил и валидации ClientService без базы данных. */
class ClientServiceTest {

    private FakeClientRepository clients;
    private FakeRepairRequestRepository requests;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clients = new FakeClientRepository(() -> requests.all());
        requests = new FakeRepairRequestRepository(clients);
        service = new ClientService(clients);
    }

    private String messageOf(Runnable action) {
        return assertThrows(BusinessException.class, action::run).getMessage();
    }

    @Test
    @DisplayName("Создание: телефон и email приводятся к единому виду")
    void create_normalizesPhoneAndEmail() {
        Client c = service.create("  Иванов Иван  ", "+7 (900) 111-22-33", "  Ivan@Mail.RU ");
        assertEquals("Иванов Иван", c.getFullName());
        assertEquals("+79001112233", c.getPhone());
        assertEquals("ivan@mail.ru", c.getEmail());
    }

    @Test
    @DisplayName("Создание: email необязателен")
    void create_withoutEmail_ok() {
        assertNull(service.create("Иванов Иван", "+79001112233", "  ").getEmail());
    }

    @Test
    @DisplayName("Создание: пустое ФИО отклоняется с указанием поля")
    void create_blankName_rejected() {
        assertTrue(messageOf(() -> service.create("   ", "+79001112233", null)).startsWith("ФИО"));
    }

    @Test
    @DisplayName("Создание: слишком длинное ФИО отклоняется до обращения к БД")
    void create_tooLongName_rejected() {
        assertTrue(messageOf(() -> service.create("а".repeat(151), "+79001112233", null)).contains("максимум 150"));
    }

    @Test
    @DisplayName("Создание: телефон 'abc' и слишком короткий/длинный номер отклоняются")
    void create_badPhone_rejected() {
        assertTrue(messageOf(() -> service.create("Иванов Иван", "abc", null)).startsWith("Телефон"));
        assertTrue(messageOf(() -> service.create("Иванов Иван", "12345", null)).startsWith("Телефон"));
        assertTrue(messageOf(() -> service.create("Иванов Иван", "+7900111223344556677", null)).startsWith("Телефон"));
    }

    @Test
    @DisplayName("Создание: неверный формат email отклоняется")
    void create_badEmail_rejected() {
        assertTrue(messageOf(() -> service.create("Иванов Иван", "+79001112233", "не-почта")).startsWith("Email"));
    }

    @Test
    @DisplayName("Уникальность: один и тот же номер в разной записи считается дубликатом")
    void create_duplicatePhone_rejectedEvenWithDifferentFormatting() {
        service.create("Иванов Иван", "+79001112233", null);
        assertTrue(messageOf(() -> service.create("Петров Пётр", "+7 900 111-22-33", null)).contains("уже принадлежит"));
    }

    @Test
    @DisplayName("Уникальность: email сравнивается без учёта регистра")
    void create_duplicateEmail_caseInsensitive() {
        service.create("Иванов Иван", "+79001112233", "a@b.ru");
        assertTrue(messageOf(() -> service.create("Петров Пётр", "+79002223344", "A@B.RU")).startsWith("Email"));
    }

    @Test
    @DisplayName("Изменение: можно оставить свой же телефон, но нельзя занять чужой")
    void update_uniquenessIgnoresOwnRecord() {
        Client first = service.create("Иванов Иван", "+79001112233", null);
        Client second = service.create("Петров Пётр", "+79002223344", null);

        service.update(first.getId(), "Иванов Иван Иванович", "+79001112233", null); // свой номер — можно
        assertEquals("Иванов Иван Иванович", service.getById(first.getId()).getFullName());

        assertTrue(messageOf(() -> service.update(second.getId(), "Петров Пётр", "+79001112233", null))
                .contains("уже принадлежит"));
    }

    @Test
    @DisplayName("Изменение: при ошибке валидации сохранённая запись не меняется")
    void update_invalidInput_leavesStoredRecordUntouched() {
        Client c = service.create("Иванов Иван", "+79001112233", null);
        assertThrows(BusinessException.class, () -> service.update(c.getId(), "Новое Имя", "abc", null));
        assertEquals("Иванов Иван", service.getById(c.getId()).getFullName());
    }

    @Test
    @DisplayName("Получение несуществующего клиента -> EntityNotFoundException")
    void getById_missing_throwsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.getById(999));
    }

    @Test
    @DisplayName("Удаление: клиент без заявок удаляется")
    void delete_withoutRequests_ok() {
        Client c = service.create("Иванов Иван", "+79001112233", null);
        service.delete(c.getId());
        assertThrows(EntityNotFoundException.class, () -> service.getById(c.getId()));
    }

    @Test
    @DisplayName("Удаление: нельзя удалить клиента с незавершёнными заявками")
    void delete_withActiveRequests_rejected() {
        Client c = service.create("Иванов Иван", "+79001112233", null);
        requests.save(new RepairRequest(c.getId(), "ПК", "не включается", Priority.NORMAL));
        assertTrue(messageOf(() -> service.delete(c.getId())).contains("незавершённые"));
    }

    @Test
    @DisplayName("Удаление: нельзя удалить клиента, у которого осталась история завершённых заявок")
    void delete_withHistoryOnly_rejectedWithReadableMessage() {
        Client c = service.create("Иванов Иван", "+79001112233", null);
        RepairRequest done = new RepairRequest(c.getId(), "ПК", "не включается", Priority.NORMAL);
        done.setStatus(RequestStatus.DONE);
        requests.save(done);
        assertTrue(messageOf(() -> service.delete(c.getId())).contains("история"));
    }
}
