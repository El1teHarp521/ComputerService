# Компьютерный сервис — КР2 (JavaFX)

Настольное приложение для учёта клиентов и заявок на компьютерный ремонт.
Продолжение проекта из КР1: **та же предметная область, та же база PostgreSQL, те же
сущности и бизнес-правила** — но вместо консольного меню теперь графический интерфейс на JavaFX.

Приложение подключается к базе **напрямую по JDBC** (без REST API и без Spring — они появятся в КР3).

```
View (FXML + CSS) → Controller → Service → Repository (JDBC) → PostgreSQL
```

## Что нужно

| Инструмент | Версия | Зачем |
|---|---|---|
| Java (JDK) | 21 | требование КР2 |
| Maven | 3.9+ | сборка, запуск, тесты |
| Docker Desktop | любая свежая | самый простой способ поднять PostgreSQL |
| IntelliJ IDEA | Community | удобнее всего для JavaFX (VS Code тоже подойдёт) |

## Быстрый старт

### 1. Поднять базу данных (любой из вариантов)

**А) Docker Compose из этой папки — рекомендуется.** Создаёт БД и сам загружает схему и тестовые данные:

```bash
docker compose up -d --wait
```

> Если порт 5432 занят контейнером `pg-service` из КР1 — остановите его: `docker stop pg-service`.
> Если база из КР1 уже запущена и содержит данные, этот шаг не нужен — просто используйте её.

**Б) Локальный PostgreSQL или старый контейнер из КР1** — создайте базу `computer_service` и выполните скрипты:

```bash
psql -h localhost -U postgres -d computer_service -f db/schema.sql
psql -h localhost -U postgres -d computer_service -f db/seed.sql
```

### 2. Проверить параметры подключения

Файл `src/main/resources/database.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/computer_service
db.user=postgres
db.password=postgres
```

Они должны совпадать с настройками базы из КР1 (при необходимости поправьте порт, пользователя, пароль).

### 3. Запустить

```bash
mvn javafx:run          # любая ОС
./run.sh                # Linux / macOS
powershell -ExecutionPolicy Bypass -File .\run.ps1     # Windows
```

В IntelliJ IDEA: откройте `javafx-client/pom.xml` как проект (File → Open), затем запустите класс
`ComputerServiceLauncher` (именно его, а не `ComputerServiceFxApp` — см. «Запуск из IDE» ниже).

### 4. Автотесты (база данных не нужна)

```bash
mvn test
```

## Возможности

- **Заявки** — таблица, добавление, изменение (двойной щелчок по строке), удаление с подтверждением.
- **Клиенты** — таблица, добавление, изменение, удаление; ниже — **заявки выбранного клиента** (связь 1:N).
- **Сведения о заявке** — боковая панель: контакты клиента, полное описание, даты.
- **Поиск** — одно поле ищет сразу по описанию неисправности, типу устройства и ФИО клиента (без учёта регистра).
- **Фильтры** — статус, приоритет, период дат создания. Условия **комбинируются** между собой и с поиском.
- **Сортировка** — выпадающий список (по номеру, новые/старые, стоимость ↑/↓, приоритет) и клик по заголовку колонки.
- **Статистика** — 11 показателей и диаграмма «Заявки по статусам».
- **Экспорт в Excel** — выгружается **текущий набор данных**: ровно то, что показано в таблице (с учётом фильтров и сортировки). Путь выбирается через системный диалог.
- Строки заявок раскрашены: выполненные — зелёные, отменённые — серые, срочные — красные.

## Структура проекта

```
javafx-client/
├── pom.xml                       Maven: JavaFX 21, PostgreSQL JDBC, Apache POI, JUnit
├── docker-compose.yml            PostgreSQL с автозагрузкой схемы и тестовых данных
├── run.sh / run.ps1              запуск приложения
├── db/
│   ├── schema.sql                создание таблиц (PK, FK, NOT NULL, UNIQUE, CHECK)
│   └── seed.sql                  тестовые данные: 5 клиентов, 10 заявок, 6 статусов
└── src/
    ├── main/resources/
    │   ├── database.properties   параметры подключения (пароль не в коде)
    │   └── ru/mirea/computerservice/fx/
    │       ├── MainView.fxml            главное окно
    │       ├── RequestFormDialog.fxml   форма заявки
    │       ├── ClientFormDialog.fxml    форма клиента
    │       └── app.css                  оформление
    ├── main/java/ru/mirea/computerservice/
    │   ├── fx/                          ← НОВОЕ в КР2: слой интерфейса
    │   │   ├── ComputerServiceLauncher  точка входа (main)
    │   │   ├── ComputerServiceFxApp     Application: Stage + Scene
    │   │   ├── controller/              MainController, RequestFormDialogController, ClientFormDialogController
    │   │   └── util/                    FxTasks (фоновые задачи), Ui (диалоги)
    │   ├── model/        Client, RepairRequest, RequestStatus, Priority
    │   ├── repository/   интерфейсы + JDBC-реализации (единственное место с SQL)
    │   ├── service/      ClientService, RepairRequestService, Validation, RequestSortOrder
    │   ├── exception/    BusinessException, EntityNotFoundException, DatabaseException
    │   └── util/         DatabaseManager, ExcelExporter
    └── test/java/…/service/             30 тестов сервисов (поддельные репозитории в памяти)
```

## Как это связано с КР1

Пакеты `model`, `repository`, `service`, `exception`, `util` перенесены из КР1; поменялся только
слой интерфейса (консоль → JavaFX). При переносе исправлены найденные недостатки — см. список ниже.

### Что изменилось по сравнению с КР1

| Что | Было в КР1 | Стало в КР2 |
|---|---|---|
| Проверка ввода | только «не пусто» и уникальность телефона при создании | длины (по полям таблиц), формат телефона и email, диапазон стоимости, нормализация, уникальность телефона **и email** при создании и изменении |
| Ошибки пользователю | при нарушении ограничения БД показывался текст PostgreSQL | все ошибки ввода и правил — понятные сообщения с названием поля |
| Удаление клиента | при наличии только завершённых заявок падало на внешнем ключе | понятное сообщение: «сохранена история заявок» |
| Изменение клиента | сервис принимал уже изменённый объект | принимает значения, проверяет, потом меняет запись — таблица не «портится» при ошибке |
| Создание заявки | стоимость не передавалась | стоимость задаётся сразу при создании и проверяется |
| Поиск | отдельные методы, `%` и `_` в тексте работали как шаблон | один поиск по трём полям; спецсимволы экранируются |
| Фильтры | каждый заменял предыдущий | комбинируются |
| Работа с БД из интерфейса | — | в фоновых потоках (`Task`), окно не «зависает» |

## Запуск из IDE

Если запускать класс, унаследованный от `javafx.application.Application`, при подключении JavaFX как обычной
библиотеки JVM сообщает «JavaFX runtime components are missing». Поэтому у проекта есть отдельный
`ComputerServiceLauncher` без наследования от `Application` — запускайте его. `mvn javafx:run` тоже стартует с него.

## Частые проблемы

| Симптом | Причина и решение |
|---|---|
| При старте: «Не удалось подключиться к базе данных» | PostgreSQL не запущен или неверные параметры в `database.properties`. Проверьте `docker ps` |
| `port is already allocated` при `docker compose up` | порт 5432 занят (например, `pg-service` из КР1): `docker stop pg-service` |
| Пустые таблицы | база создана без `seed.sql`; выполните его (см. шаг 1Б) или `docker compose down -v` и поднимите заново |
| «JavaFX runtime components are missing» | запускайте `ComputerServiceLauncher` или `mvn javafx:run` |
| `release version 21 not supported` | Maven использует старую Java; проверьте `mvn -version` и `JAVA_HOME` |

## Перед защитой

- [ ] Хотя бы один раз развернуть демо-систему из архива `student-hub-suite` (по ТЗ это обязательно): Docker → `backend/start-compose.ps1` → проверить `http://localhost:8080/` и `/actuator/health` → только затем `javafx-client` из архива.
- [ ] Прогнать у **всех** участников: `mvn test` и `mvn javafx:run`.
- [ ] Подготовить: исходный код, `pom.xml`, `db/schema.sql`, `db/seed.sql`, экспортированный `.xlsx` (создать кнопкой «Экспорт в Excel»), демонстрацию.
- [ ] Прочитать `DEFENSE.md` — там ответы на все вопросы из раздела «На защите необходимо уметь объяснить».
