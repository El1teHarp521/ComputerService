# Компьютерный сервис — КР1

Консольная информационная система учёта клиентов и заявок на компьютерный ремонт.
Java 17, JDBC, PostgreSQL, Maven, Apache POI (экспорт в Excel).

## Структура проекта

```
computer-service-kr1/
├── pom.xml
├── db/
│   ├── schema.sql   — создание таблиц
│   └── seed.sql     — тестовые данные
└── src/main/
    ├── resources/database.properties  — параметры подключения к БД
    └── java/ru/mirea/computerservice/
        ├── Main.java
        ├── model/        Client, RepairRequest, RequestStatus, Priority
        ├── repository/    интерфейсы + JDBC-реализации
        ├── service/       бизнес-логика
        ├── exception/     BusinessException, EntityNotFoundException, DatabaseException
        └── util/          DatabaseManager, ExcelExporter, InputHelper
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