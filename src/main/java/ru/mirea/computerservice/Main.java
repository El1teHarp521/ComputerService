package ru.mirea.computerservice;

import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.exception.DatabaseException;
import ru.mirea.computerservice.exception.EntityNotFoundException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.repository.ClientRepository;
import ru.mirea.computerservice.repository.ClientRepositoryJdbc;
import ru.mirea.computerservice.repository.RepairRequestRepository;
import ru.mirea.computerservice.repository.RepairRequestRepositoryJdbc;
import ru.mirea.computerservice.service.ClientService;
import ru.mirea.computerservice.service.RepairRequestService;
import ru.mirea.computerservice.util.ExcelExporter;
import ru.mirea.computerservice.util.InputHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final ClientRepository clientRepository = new ClientRepositoryJdbc();
    private static final ClientService clientService = new ClientService(clientRepository);
    private static final RepairRequestRepository requestRepository = new RepairRequestRepositoryJdbc();
    private static final RepairRequestService requestService =
            new RepairRequestService(requestRepository, clientService);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = InputHelper.readInt(scanner, "Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> clientsMenu();
                    case 2 -> requestsMenu();
                    case 3 -> searchMenu();
                    case 4 -> filterMenu();
                    case 5 -> showStatistics();
                    case 6 -> exportMenu();
                    case 7 -> printTables();
                    case 0 -> running = false;
                    default -> System.out.println("Неверный пункт меню.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }
        System.out.println("Работа программы завершена.");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("======================================== СИСТЕМА КОМПЬЮТЕРНОГО СЕРВИСА");
        System.out.println("1. Клиенты");
        System.out.println("2. Заявки");
        System.out.println("3. Поиск");
        System.out.println("4. Фильтрация / сортировка");
        System.out.println("5. Статистика");
        System.out.println("6. Экспорт данных");
        System.out.println("7. Вывести таблицы базы данных");
        System.out.println("0. Выход");
    }

    // ---------------- Клиенты ----------------

    private static void clientsMenu() {
        System.out.println();
        System.out.println("---- КЛИЕНТЫ ----");
        System.out.println("1. Список клиентов");
        System.out.println("2. Добавить клиента");
        System.out.println("3. Изменить клиента");
        System.out.println("4. Удалить клиента");
        System.out.println("5. Поиск клиента по имени");
        System.out.println("0. Назад");
        int choice = InputHelper.readInt(scanner, "Выберите действие: ");
        switch (choice) {
            case 1 -> printClients(clientService.getAll());
            case 2 -> addClient();
            case 3 -> editClient();
            case 4 -> deleteClient();
            case 5 -> {
                String query = InputHelper.readString(scanner, "Введите часть имени: ");
                printClients(clientService.searchByName(query));
            }
            case 0 -> {
            }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    private static void addClient() {
        String name = InputHelper.readNonEmptyString(scanner, "ФИО клиента: ");
        String phone = InputHelper.readNonEmptyString(scanner, "Телефон: ");
        String email = InputHelper.readString(scanner, "Email (можно оставить пустым): ");
        Client client = clientService.create(name, phone, email.isEmpty() ? null : email);
        System.out.println("Клиент создан: " + client);
    }

    private static void editClient() {
        int id = InputHelper.readInt(scanner, "ID клиента: ");
        Client client = clientService.getById(id);
        System.out.println("Текущие данные: " + client);
        String name = InputHelper.readString(scanner, "Новое ФИО (Enter — оставить прежнее): ");
        if (!name.isEmpty()) client.setFullName(name);
        String phone = InputHelper.readString(scanner, "Новый телефон (Enter — оставить прежний): ");
        if (!phone.isEmpty()) client.setPhone(phone);
        String email = InputHelper.readString(scanner, "Новый email (Enter — оставить прежний): ");
        if (!email.isEmpty()) client.setEmail(email);
        clientService.update(client);
        System.out.println("Клиент обновлён.");
    }

    private static void deleteClient() {
        int id = InputHelper.readInt(scanner, "ID клиента: ");
        clientService.delete(id);
        System.out.println("Клиент удалён.");
    }

    private static void printClients(List<Client> clients) {
        if (clients.isEmpty()) {
            System.out.println("Список пуст.");
            return;
        }
        clients.forEach(System.out::println);
    }

    // ---------------- Заявки ----------------

    private static void requestsMenu() {
        System.out.println();
        System.out.println("---- ЗАЯВКИ ----");
        System.out.println("1. Список заявок");
        System.out.println("2. Заявка по ID");
        System.out.println("3. Добавить заявку");
        System.out.println("4. Изменить статус заявки");
        System.out.println("5. Указать стоимость ремонта");
        System.out.println("6. Удалить заявку");
        System.out.println("0. Назад");
        int choice = InputHelper.readInt(scanner, "Выберите действие: ");
        switch (choice) {
            case 1 -> printRequests(requestService.getAll());
            case 2 -> {
                int id = InputHelper.readInt(scanner, "ID заявки: ");
                System.out.println(requestService.getById(id));
            }
            case 3 -> addRequest();
            case 4 -> changeStatus();
            case 5 -> setCost();
            case 6 -> {
                int id = InputHelper.readInt(scanner, "ID заявки: ");
                requestService.delete(id);
                System.out.println("Заявка удалена.");
            }
            case 0 -> {
            }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    private static void addRequest() {
        int clientId = InputHelper.readInt(scanner, "ID клиента: ");
        String deviceType = InputHelper.readNonEmptyString(scanner, "Тип устройства: ");
        String description = InputHelper.readNonEmptyString(scanner, "Описание неисправности: ");
        Priority priority = choosePriority();
        RepairRequest request = requestService.create(clientId, deviceType, description, priority);
        System.out.println("Заявка создана: " + request);
    }

    private static Priority choosePriority() {
        System.out.println("Приоритет: 1-Низкий 2-Обычный 3-Высокий 4-Срочный");
        int p = InputHelper.readInt(scanner, "Выберите: ");
        return switch (p) {
            case 1 -> Priority.LOW;
            case 3 -> Priority.HIGH;
            case 4 -> Priority.URGENT;
            default -> Priority.NORMAL;
        };
    }

    private static void changeStatus() {
        int id = InputHelper.readInt(scanner, "ID заявки: ");
        System.out.println("Статус: 1-NEW 2-DIAGNOSTICS 3-IN_PROGRESS 4-WAITING_PARTS 5-DONE 6-CANCELLED");
        int s = InputHelper.readInt(scanner, "Выберите: ");
        RequestStatus status = switch (s) {
            case 1 -> RequestStatus.NEW;
            case 2 -> RequestStatus.DIAGNOSTICS;
            case 3 -> RequestStatus.IN_PROGRESS;
            case 4 -> RequestStatus.WAITING_PARTS;
            case 5 -> RequestStatus.DONE;
            case 6 -> RequestStatus.CANCELLED;
            default -> throw new BusinessException("Некорректный статус");
        };
        requestService.changeStatus(id, status);
        System.out.println("Статус обновлён.");
    }

    private static void setCost() {
        int id = InputHelper.readInt(scanner, "ID заявки: ");
        BigDecimal cost = InputHelper.readBigDecimal(scanner, "Стоимость ремонта: ");
        requestService.setCost(id, cost);
        System.out.println("Стоимость обновлена.");
    }

    private static void printRequests(List<RepairRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("Список пуст.");
            return;
        }
        requests.forEach(System.out::println);
    }

    // ---------------- Поиск ----------------

    private static void searchMenu() {
        System.out.println();
        System.out.println("---- ПОИСК ----");
        System.out.println("1. По описанию неисправности");
        System.out.println("2. По имени клиента");
        System.out.println("0. Назад");
        int choice = InputHelper.readInt(scanner, "Выберите действие: ");
        switch (choice) {
            case 1 -> {
                String kw = InputHelper.readString(scanner, "Ключевое слово: ");
                printRequests(requestService.searchByDescription(kw));
            }
            case 2 -> {
                String name = InputHelper.readString(scanner, "Часть имени клиента: ");
                printRequests(requestService.searchByClientName(name));
            }
            case 0 -> {
            }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    // ---------------- Фильтрация / сортировка ----------------

    private static void filterMenu() {
        System.out.println();
        System.out.println("---- ФИЛЬТРАЦИЯ / СОРТИРОВКА ----");
        System.out.println("1. По статусу");
        System.out.println("2. По приоритету");
        System.out.println("3. По диапазону дат");
        System.out.println("4. Сортировка по дате создания");
        System.out.println("5. Сортировка по стоимости");
        System.out.println("0. Назад");
        int choice = InputHelper.readInt(scanner, "Выберите действие: ");
        switch (choice) {
            case 1 -> {
                System.out.println("Статус: 1-NEW 2-DIAGNOSTICS 3-IN_PROGRESS 4-WAITING_PARTS 5-DONE 6-CANCELLED");
                int s = InputHelper.readInt(scanner, "Выберите: ");
                if (s < 1 || s > RequestStatus.values().length) {
                    throw new BusinessException("Некорректный статус");
                }
                printRequests(requestService.filterByStatus(RequestStatus.values()[s - 1]));
            }
            case 2 -> printRequests(requestService.filterByPriority(choosePriority()));
            case 3 -> {
                LocalDate from = LocalDate.parse(InputHelper.readNonEmptyString(scanner, "Дата от (гггг-мм-дд): "));
                LocalDate to = LocalDate.parse(InputHelper.readNonEmptyString(scanner, "Дата до (гггг-мм-дд): "));
                printRequests(requestService.filterByDateRange(from, to));
            }
            case 4 -> printRequests(requestService.sortByDate());
            case 5 -> printRequests(requestService.sortByCost());
            case 0 -> {
            }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    // ---------------- Статистика ----------------

    private static void showStatistics() {
        System.out.println();
        System.out.println("---- СТАТИСТИКА ----");
        Map<String, Object> stats = requestService.getStatistics();
        stats.forEach((key, value) -> System.out.printf("%-32s %s%n", key + ":", value));
    }

    // ---------------- Экспорт ----------------

    private static void exportMenu() {
        String fileName = InputHelper.readNonEmptyString(scanner, "Имя файла для экспорта (например export.xlsx): ");
        try {
            ExcelExporter.exportRequests(requestService.getAll(), fileName);
            System.out.println("Экспорт выполнен: " + fileName);
        } catch (java.io.IOException e) {
            System.out.println("Ошибка экспорта: " + e.getMessage());
        }
    }

    // ---------------- Таблицы БД ----------------

    private static void printTables() {
        System.out.println();
        System.out.println("---- ТАБЛИЦА: clients ----");
        printClients(clientService.getAll());
        System.out.println();
        System.out.println("---- ТАБЛИЦА: repair_requests ----");
        printRequests(requestService.getAll());
    }
}
