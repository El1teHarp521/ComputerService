package ru.mirea.computerservice; // Пакет, в котором находится класс

import ru.mirea.computerservice.exception.BusinessException; // Импорт исключения для нарушения бизнес-правил
import ru.mirea.computerservice.exception.DatabaseException; // Импорт исключения для ошибок БД
import ru.mirea.computerservice.exception.EntityNotFoundException; // Импорт исключения "сущность не найдена"
import ru.mirea.computerservice.model.Client; // Импорт модели Client
import ru.mirea.computerservice.model.Priority; // Импорт enum Priority
import ru.mirea.computerservice.model.RepairRequest; // Импорт модели RepairRequest
import ru.mirea.computerservice.model.RequestStatus; // Импорт enum RequestStatus
import ru.mirea.computerservice.repository.ClientRepository; // Импорт интерфейса репозитория клиентов
import ru.mirea.computerservice.repository.ClientRepositoryJdbc; // Импорт JDBC-реализации репозитория клиентов
import ru.mirea.computerservice.repository.RepairRequestRepository; // Импорт интерфейса репозитория заявок
import ru.mirea.computerservice.repository.RepairRequestRepositoryJdbc; // Импорт JDBC-реализации репозитория заявок
import ru.mirea.computerservice.service.ClientService; // Импорт сервиса клиентов
import ru.mirea.computerservice.service.RepairRequestService; // Импорт сервиса заявок
import ru.mirea.computerservice.util.ExcelExporter; // Импорт утилиты экспорта в Excel
import ru.mirea.computerservice.util.InputHelper; // Импорт утилиты ввода

import java.math.BigDecimal; // Импорт BigDecimal для работы с деньгами
import java.nio.charset.Charset; // Импорт Charset для работы с кодировками
import java.time.LocalDate; // Импорт LocalDate для работы с датами
import java.util.List; // Импорт List
import java.util.Map; // Импорт Map для статистики
import java.util.Scanner; // Импорт Scanner для чтения ввода

public class Main { // Главный класс программы — точка входа

    // Умная инициализация сканера: читает напрямую из Unicode-консоли Windows
    private static final Scanner scanner = createScanner(); // Статическое поле — Scanner, создается один раз

    private static Scanner createScanner() { // Метод создания Scanner с правильной кодировкой
        if (System.console() != null) { // Если программа запущена из реальной консоли
            return new Scanner(System.console().reader()); // Используем reader консоли (правильная кодировка)
        }
        // Если запущено внутри IDE без консоли
        try {
            String nativeEncoding = System.getProperty("native.encoding", Charset.defaultCharset().name()); // Получаем кодировку системы
            return new Scanner(System.in, nativeEncoding); // Создаем Scanner с этой кодировкой
        } catch (Exception e) { // Если что-то пошло не так
            return new Scanner(System.in); // Fallback — обычный Scanner
        }
    }

    private static final ClientRepository clientRepository = new ClientRepositoryJdbc(); // Создаем репозиторий клиентов (JDBC)
    private static final ClientService clientService = new ClientService(clientRepository); // Создаем сервис клиентов, передаем репозиторий
    private static final RepairRequestRepository requestRepository = new RepairRequestRepositoryJdbc(); // Создаем репозиторий заявок (JDBC)
    private static final RepairRequestService requestService =
            new RepairRequestService(requestRepository, clientService); // Создаем сервис заявок, передаем репозиторий и сервис клиентов

    public static void main(String[] args) { // Точка входа в программу
        boolean running = true; // Флаг работы программы
        while (running) { // Главный цикл программы
            printMainMenu(); // Выводим главное меню
            int choice = InputHelper.readInt(scanner, "Выберите действие: "); // Читаем выбор пользователя
            try { // Обрабатываем возможные исключения
                switch (choice) { // Выбор действия
                    case 1 -> clientsMenu(); // Меню клиентов
                    case 2 -> requestsMenu(); // Меню заявок
                    case 3 -> searchMenu(); // Меню поиска
                    case 4 -> filterMenu(); // Меню фильтрации/сортировки
                    case 5 -> showStatistics(); // Показать статистику
                    case 6 -> exportMenu(); // Меню экспорта
                    case 7 -> printTables(); // Вывести таблицы БД
                    case 0 -> running = false; // Выход из программы
                    default -> System.out.println("Неверный пункт меню."); // Неверный ввод
                }
            } catch (BusinessException | EntityNotFoundException e) { // Ловим бизнес-исключения
                System.out.println("Ошибка: " + e.getMessage()); // Выводим сообщение
            } catch (DatabaseException e) { // Ловим ошибки БД
                System.out.println("Ошибка базы данных: " + e.getMessage()); // Выводим сообщение
            } catch (Exception e) { // Ловим все остальные исключения
                System.out.println("Непредвиденная ошибка: " + e.getMessage()); // Выводим сообщение
            }
        }
        System.out.println("Работа программы завершена."); // Сообщение о завершении
    }

    private static void printMainMenu() { // Метод вывода главного меню
        System.out.println(); // Пустая строка
        System.out.println("======================================== СИСТЕМА КОМПЬЮТЕРНОГО СЕРВИСА"); // Заголовок
        System.out.println("1. Клиенты"); // Пункт 1
        System.out.println("2. Заявки"); // Пункт 2
        System.out.println("3. Поиск"); // Пункт 3
        System.out.println("4. Фильтрация / сортировка"); // Пункт 4
        System.out.println("5. Статистика"); // Пункт 5
        System.out.println("6. Экспорт данных"); // Пункт 6
        System.out.println("7. Вывести таблицы базы данных"); // Пункт 7
        System.out.println("0. Выход"); // Пункт 0
    }

    // ---------------- Клиенты ----------------

    private static void clientsMenu() { // Меню работы с клиентами
        System.out.println(); // Пустая строка
        System.out.println("---- КЛИЕНТЫ ----"); // Заголовок
        System.out.println("1. Список клиентов"); // Пункт 1
        System.out.println("2. Добавить клиента"); // Пункт 2
        System.out.println("3. Изменить клиента"); // Пункт 3
        System.out.println("4. Удалить клиента"); // Пункт 4
        System.out.println("5. Поиск клиента по имени"); // Пункт 5
        System.out.println("0. Назад"); // Пункт 0
        int choice = InputHelper.readInt(scanner, "Выберите действие: "); // Читаем выбор
        switch (choice) { // Выбор действия
            case 1 -> printClients(clientService.getAll()); // Показать всех клиентов
            case 2 -> addClient(); // Добавить клиента
            case 3 -> editClient(); // Изменить клиента
            case 4 -> deleteClient(); // Удалить клиента
            case 5 -> { // Поиск по имени
                String query = InputHelper.readString(scanner, "Введите часть имени: "); // Читаем часть имени
                printClients(clientService.searchByName(query)); // Ищем и выводим
            }
            case 0 -> { // Назад
            }
            default -> System.out.println("Неверный пункт меню."); // Неверный ввод
        }
    }

    private static void addClient() { // Метод добавления клиента
        String name = InputHelper.readNonEmptyString(scanner, "ФИО клиента: "); // Читаем ФИО (не пустое)
        String phone = InputHelper.readNonEmptyString(scanner, "Телефон: "); // Читаем телефон (не пустой)
        String email = InputHelper.readString(scanner, "Email (можно оставить пустым): "); // Читаем email (может быть пустым)
        Client client = clientService.create(name, phone, email.isEmpty() ? null : email); // Создаем клиента (email = null, если пусто)
        System.out.println("Клиент создан: " + client); // Выводим результат
    }

    private static void editClient() { // Метод редактирования клиента
        int id = InputHelper.readInt(scanner, "ID клиента: "); // Читаем ID
        Client client = clientService.getById(id); // Получаем клиента (или исключение)
        System.out.println("Текущие данные: " + client); // Показываем текущие данные
        String name = InputHelper.readString(scanner, "Новое ФИО (Enter — оставить прежнее): "); // Читаем новое ФИО
        if (!name.isEmpty()) client.setFullName(name); // Если не пусто — обновляем
        String phone = InputHelper.readString(scanner, "Новый телефон (Enter — оставить прежний): "); // Читаем новый телефон
        if (!phone.isEmpty()) client.setPhone(phone); // Если не пусто — обновляем
        String email = InputHelper.readString(scanner, "Новый email (Enter — оставить прежний): "); // Читаем новый email
        if (!email.isEmpty()) client.setEmail(email); // Если не пусто — обновляем
        clientService.update(client); // Сохраняем изменения
        System.out.println("Клиент обновлён."); // Сообщение
    }

    private static void deleteClient() { // Метод удаления клиента
        int id = InputHelper.readInt(scanner, "ID клиента: "); // Читаем ID
        clientService.delete(id); // Удаляем через сервис
        System.out.println("Клиент удалён."); // Сообщение
    }

    private static void printClients(List<Client> clients) { // Метод вывода списка клиентов
        if (clients.isEmpty()) { // Если список пуст
            System.out.println("Список пуст."); // Сообщение
            return; // Выходим
        }
        clients.forEach(System.out::println); // Выводим каждого клиента
    }

    // ---------------- Заявки ----------------

    private static void requestsMenu() { // Меню работы с заявками
        System.out.println(); // Пустая строка
        System.out.println("---- ЗАЯВКИ ----"); // Заголовок
        System.out.println("1. Список заявок"); // Пункт 1
        System.out.println("2. Заявка по ID"); // Пункт 2
        System.out.println("3. Добавить заявку"); // Пункт 3
        System.out.println("4. Изменить статус заявки"); // Пункт 4
        System.out.println("5. Указать стоимость ремонта"); // Пункт 5
        System.out.println("6. Удалить заявку"); // Пункт 6
        System.out.println("0. Назад"); // Пункт 0
        int choice = InputHelper.readInt(scanner, "Выберите действие: "); // Читаем выбор
        switch (choice) { // Выбор действия
            case 1 -> printRequests(requestService.getAll()); // Показать все заявки
            case 2 -> { // Заявка по ID
                int id = InputHelper.readInt(scanner, "ID заявки: "); // Читаем ID
                System.out.println(requestService.getById(id)); // Выводим заявку
            }
            case 3 -> addRequest(); // Добавить заявку
            case 4 -> changeStatus(); // Изменить статус
            case 5 -> setCost(); // Установить стоимость
            case 6 -> { // Удалить заявку
                int id = InputHelper.readInt(scanner, "ID заявки: "); // Читаем ID
                requestService.delete(id); // Удаляем
                System.out.println("Заявка удалена."); // Сообщение
            }
            case 0 -> { // Назад
            }
            default -> System.out.println("Неверный пункт меню."); // Неверный ввод
        }
    }

    private static void addRequest() { // Метод добавления заявки
        int clientId = InputHelper.readInt(scanner, "ID клиента: "); // Читаем ID клиента
        String deviceType = InputHelper.readNonEmptyString(scanner, "Тип устройства: "); // Читаем тип устройства
        String description = InputHelper.readNonEmptyString(scanner, "Описание неисправности: "); // Читаем описание
        Priority priority = choosePriority(); // Выбираем приоритет
        RepairRequest request = requestService.create(clientId, deviceType, description, priority); // Создаем заявку
        System.out.println("Заявка создана: " + request); // Выводим результат
    }

    private static Priority choosePriority() { // Метод выбора приоритета
        System.out.println("Приоритет: 1-Низкий 2-Обычный 3-Высокий 4-Срочный"); // Подсказка
        int p = InputHelper.readInt(scanner, "Выберите: "); // Читаем выбор
        return switch (p) { // Возвращаем соответствующий enum
            case 1 -> Priority.LOW; // 1 — низкий
            case 3 -> Priority.HIGH; // 3 — высокий
            case 4 -> Priority.URGENT; // 4 — срочный
            default -> Priority.NORMAL; // 2 и остальное — обычный
        };
    }

    private static void changeStatus() { // Метод изменения статуса
        int id = InputHelper.readInt(scanner, "ID заявки: "); // Читаем ID заявки
        System.out.println("Статус: 1-NEW 2-DIAGNOSTICS 3-IN_PROGRESS 4-WAITING_PARTS 5-DONE 6-CANCELLED"); // Подсказка
        int s = InputHelper.readInt(scanner, "Выберите: "); // Читаем выбор
        RequestStatus status = switch (s) { // Преобразуем число в enum
            case 1 -> RequestStatus.NEW; // 1 — NEW
            case 2 -> RequestStatus.DIAGNOSTICS; // 2 — DIAGNOSTICS
            case 3 -> RequestStatus.IN_PROGRESS; // 3 — IN_PROGRESS
            case 4 -> RequestStatus.WAITING_PARTS; // 4 — WAITING_PARTS
            case 5 -> RequestStatus.DONE; // 5 — DONE
            case 6 -> RequestStatus.CANCELLED; // 6 — CANCELLED
            default -> throw new BusinessException("Некорректный статус"); // Иначе — исключение
        };
        requestService.changeStatus(id, status); // Меняем статус через сервис
        System.out.println("Статус обновлён."); // Сообщение
    }

    private static void setCost() { // Метод установки стоимости
        int id = InputHelper.readInt(scanner, "ID заявки: "); // Читаем ID заявки
        BigDecimal cost = InputHelper.readBigDecimal(scanner, "Стоимость ремонта: "); // Читаем стоимость
        requestService.setCost(id, cost); // Устанавливаем через сервис
        System.out.println("Стоимость обновлена."); // Сообщение
    }

    private static void printRequests(List<RepairRequest> requests) { // Метод вывода списка заявок
        if (requests.isEmpty()) { // Если список пуст
            System.out.println("Список пуст."); // Сообщение
            return; // Выходим
        }
        requests.forEach(System.out::println); // Выводим каждую заявку
    }

    // ---------------- Поиск ----------------

    private static void searchMenu() { // Меню поиска
        System.out.println(); // Пустая строка
        System.out.println("---- ПОИСК ----"); // Заголовок
        System.out.println("1. По описанию неисправности"); // Пункт 1
        System.out.println("2. По имени клиента"); // Пункт 2
        System.out.println("0. Назад"); // Пункт 0
        int choice = InputHelper.readInt(scanner, "Выберите действие: "); // Читаем выбор
        switch (choice) { // Выбор действия
            case 1 -> { // По описанию
                String kw = InputHelper.readString(scanner, "Ключевое слово: "); // Читаем ключевое слово
                printRequests(requestService.searchByDescription(kw)); // Ищем и выводим
            }
            case 2 -> { // По имени клиента
                String name = InputHelper.readString(scanner, "Часть имени клиента: "); // Читаем часть имени
                printRequests(requestService.searchByClientName(name)); // Ищем и выводим
            }
            case 0 -> { // Назад
            }
            default -> System.out.println("Неверный пункт меню."); // Неверный ввод
        }
    }

    // ---------------- Фильтрация / сортировка ----------------

    private static void filterMenu() { // Меню фильтрации и сортировки
        System.out.println(); // Пустая строка
        System.out.println("---- ФИЛЬТРАЦИЯ / СОРТИРОВКА ----"); // Заголовок
        System.out.println("1. По статусу"); // Пункт 1
        System.out.println("2. По приоритету"); // Пункт 2
        System.out.println("3. По диапазону дат"); // Пункт 3
        System.out.println("4. Сортировка по дате создания"); // Пункт 4
        System.out.println("5. Сортировка по стоимости"); // Пункт 5
        System.out.println("0. Назад"); // Пункт 0
        int choice = InputHelper.readInt(scanner, "Выберите действие: "); // Читаем выбор
        switch (choice) { // Выбор действия
            case 1 -> { // По статусу
                System.out.println("Статус: 1-NEW 2-DIAGNOSTICS 3-IN_PROGRESS 4-WAITING_PARTS 5-DONE 6-CANCELLED"); // Подсказка
                int s = InputHelper.readInt(scanner, "Выберите: "); // Читаем выбор
                if (s < 1 || s > RequestStatus.values().length) { // Проверка диапазона
                    throw new BusinessException("Некорректный статус"); // Исключение
                }
                printRequests(requestService.filterByStatus(RequestStatus.values()[s - 1])); // Фильтруем и выводим
            }
            case 2 -> printRequests(requestService.filterByPriority(choosePriority())); // По приоритету
            case 3 -> { // По диапазону дат
                LocalDate from = LocalDate.parse(InputHelper.readNonEmptyString(scanner, "Дата от (гггг-мм-дд): ")); // Читаем дату "от"
                LocalDate to = LocalDate.parse(InputHelper.readNonEmptyString(scanner, "Дата до (гггг-мм-дд): ")); // Читаем дату "до"
                printRequests(requestService.filterByDateRange(from, to)); // Фильтруем и выводим
            }
            case 4 -> printRequests(requestService.sortByDate()); // Сортировка по дате
            case 5 -> printRequests(requestService.sortByCost()); // Сортировка по стоимости
            case 0 -> { // Назад
            }
            default -> System.out.println("Неверный пункт меню."); // Неверный ввод
        }
    }

    // ---------------- Статистика ----------------

    private static void showStatistics() { // Метод показа статистики
        System.out.println(); // Пустая строка
        System.out.println("---- СТАТИСТИКА ----"); // Заголовок
        Map<String, Object> stats = requestService.getStatistics(); // Получаем статистику
        stats.forEach((key, value) -> System.out.printf("%-32s %s%n", key + ":", value)); // Выводим каждую пару ключ-значение
    }

    // ---------------- Экспорт ----------------

    private static void exportMenu() { // Меню экспорта
        String fileName = InputHelper.readNonEmptyString(scanner, "Имя файла для экспорта (например export.xlsx): "); // Читаем имя файла
        try {
            ExcelExporter.exportRequests(requestService.getAll(), fileName); // Экспортируем все заявки
            System.out.println("Экспорт выполнен: " + fileName); // Сообщение об успехе
        } catch (java.io.IOException e) { // Если ошибка ввода-вывода
            System.out.println("Ошибка экспорта: " + e.getMessage()); // Сообщение об ошибке
        }
    }

    // ---------------- Таблицы БД ----------------

    private static void printTables() { // Метод вывода таблиц БД
        System.out.println(); // Пустая строка
        System.out.println("---- ТАБЛИЦА: clients ----"); // Заголовок
        printClients(clientService.getAll()); // Выводим клиентов
        System.out.println(); // Пустая строка
        System.out.println("---- ТАБЛИЦА: repair_requests ----"); // Заголовок
        printRequests(requestService.getAll()); // Выводим заявки
    }
}