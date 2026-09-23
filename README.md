# Компьютерный сервис — КР1

Консольная информационная система учёта клиентов и заявок на компьютерный ремонт.
Java 17, JDBC, PostgreSQL, Maven, Apache POI (экспорт в Excel).

## Структура проекта

```
computer-service-kr1/
├── pom.xml                    # Конфигурационный файл Maven: зависимости (PostgreSQL JDBC, Apache POI), 
│                               # версия Java, плагины для сборки и запуска
│
├── README.md                  # Инструкция по установке БД и запуску проекта, чек-лист готовности к сдаче
│
├── db/                        # SQL-скрипты — представляются на защиту отдельными файлами
│   ├── schema.sql             # DDL: создание таблиц clients и repair_requests, 
│   │                           # PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK
│   └── seed.sql                # Тестовые данные: 5 клиентов, 10 заявок, 3+ статуса
│
└── src/main/
    ├── resources/
    │   └── database.properties   # Параметры подключения к БД (url, логин, пароль) — вынесены из кода
    │
    └── java/ru/mirea/computerservice/
        │
        ├── Main.java              # Точка входа. Консольное меню (UI-слой), обработка ввода и исключений.
        │                          # Не содержит SQL и бизнес-проверок — только вызывает сервисы
        │
        ├── model/                 # Предметная модель — сущности и перечисления, без логики и SQL
        │   ├── Client.java        # Сущность «Клиент»: ФИО, телефон, email — пользователь системы
        │   ├── RepairRequest.java # Основная сущность «Заявка на ремонт»: устройство, описание, статус,
        │   │                       # приоритет, стоимость; связана с Client через client_id (FK)
        │   ├── RequestStatus.java # Enum статусов заявки: NEW, DIAGNOSTICS, IN_PROGRESS, 
        │   │                       # WAITING_PARTS, DONE, CANCELLED
        │   └── Priority.java      # Enum приоритета заявки: LOW, NORMAL, HIGH, URGENT
        │
        ├── repository/            # Слой доступа к данным (DAO) — весь SQL только здесь
        │   ├── ClientRepository.java             # Интерфейс: какие операции возможны с клиентами
        │   ├── ClientRepositoryJdbc.java          # JDBC-реализация: PreparedStatement, try-with-resources
        │   ├── RepairRequestRepository.java       # Интерфейс: операции с заявками
        │   └── RepairRequestRepositoryJdbc.java   # JDBC-реализация + JOIN с clients для имени клиента
        │
        ├── service/               # Слой бизнес-логики — здесь все проверки перед сохранением/удалением
        │   ├── ClientService.java         # Проверки: обязательное ФИО/телефон, уникальность телефона,
        │   │                                # запрет удаления клиента с активными заявками
        │   └── RepairRequestService.java  # Проверки статусов и стоимости; поиск, фильтрация,
        │                                    # сортировка (Stream API); подсчёт статистики (Collectors)
        │
        ├── exception/              # Собственные исключения
        │   ├── BusinessException.java        # Нарушение бизнес-правила (пустое поле, отриц. стоимость)
        │   ├── EntityNotFoundException.java  # Запись с указанным ID не найдена
        │   └── DatabaseException.java        # Обёртка над SQLException — единая точка обработки ошибок БД
        │
        └── util/                    # Вспомогательные технические классы
            ├── DatabaseManager.java    # Единственное место, где открывается Connection к PostgreSQL
            ├── ExcelExporter.java      # Экспорт списка заявок в .xlsx через Apache POI
            └── InputHelper.java        # Безопасное чтение с консоли: не роняет программу при вводе текста
```

## Запуск с нуля

### 1. Поднять PostgreSQL

Через Docker:
```bash
docker run --name pg-service -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=computer_service -p 5432:5432 -d postgres:16
```

Или через локально установленный PostgreSQL — создайте БД `computer_service` вручную.

### 2. Создать таблицы и загрузить тестовые данные

```bash
psql -h localhost -U postgres -d computer_service -f db/schema.sql
psql -h localhost -U postgres -d computer_service -f db/seed.sql
```

(пароль — `postgres`, если использовали команду docker run выше)

### 3. Проверить настройки подключения

Откройте `src/main/resources/database.properties` и при необходимости поправьте
логин/пароль/порт под вашу конфигурацию:

```
db.url=jdbc:postgresql://localhost:5432/computer_service
db.user=postgres
db.password=postgres
```

### 4. Собрать и запустить

```bash
mvn clean compile
mvn exec:java
```

Либо через VS Code: открыть `Main.java` → кнопка Run (треугольник) над методом `main`.

## Что уже реализовано (по чек-листу ТЗ)

- Главное меню под предметную область (Клиенты / Заявки / Поиск / Фильтрация / Статистика / Экспорт / Таблицы БД)
- 2 связанные сущности: Client и RepairRequest (FK client_id)
- Полный CRUD для RepairRequest + CRUD для Client
- Enum RequestStatus и Priority
- 5 бизнес-правил: обязательное ФИО/телефон, уникальность телефона, обязательное описание/тип устройства,
  запрет отрицательной стоимости, запрет смены статуса у DONE/CANCELLED, запрет удаления клиента с активными заявками
- Обработка некорректного ввода (текст вместо числа), отсутствия записи, нарушения бизнес-правил, ошибок БД
- 2 способа поиска (по описанию, по имени клиента), 2 фильтра (статус, приоритет) + фильтр по дате,
  2 сортировки (по дате, по стоимости) — реализованы через Stream API
- Статистика (6 показателей)
- Экспорт в .xlsx через Apache POI
- Слои: Console UI (Main) → Service → Repository/JDBC → PostgreSQL
- PreparedStatement везде, try-with-resources, никакой конкатенации SQL
- PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK — в schema.sql