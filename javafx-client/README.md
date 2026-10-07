# Компьютерный сервис — КР2 (JavaFX)

Настольное графическое приложение на JavaFX, продолжающее проект из КР1.
Работает с той же базой данных PostgreSQL через собственное JDBC-подключение —
без REST API и без Spring Boot (они появятся позже, в КР3).

## Как это соотносится с КР1

Архитектура не изменилась:

```
UI-слой → Service → Repository/JDBC → PostgreSQL
```

Изменился только UI-слой: вместо консольного меню (`Main.java` + `Scanner`) теперь
окно JavaFX (`MainView.fxml` + `MainController.java`). Пакеты `model`, `repository`,
`service`, `exception`, `util` перенесены из КР1 практически без изменений —
добавлен только один новый метод `RepairRequestService.updateDetails()` для
редактирования заявки целиком из формы (в консоли это были два отдельных
пункта меню — "сменить статус" и "указать стоимость").

## Структура модуля

```
javafx-client/
├── pom.xml
└── src/main/
    ├── resources/
    │   ├── database.properties                         — подключение к БД из КР1
    │   └── ru/mirea/computerservice/fx/
    │       ├── MainView.fxml                            — главное окно
    │       ├── RequestFormDialog.fxml                   — форма заявки
    │       ├── ClientFormDialog.fxml                    — форма клиента
    │       └── app.css                                  — оформление
    └── java/ru/mirea/computerservice/
        ├── fx/
        │   ├── ComputerServiceFxApp.java                — точка входа (Application)
        │   └── controller/
        │       ├── MainController.java                  — логика главного окна
        │       ├── RequestFormDialogController.java      — логика формы заявки
        │       └── ClientFormDialogController.java       — логика формы клиента
        ├── model/        — перенесено из КР1 без изменений
        ├── repository/   — перенесено из КР1 без изменений
        ├── service/       — ClientService без изменений, в RepairRequestService
        │                     добавлен updateDetails()
        ├── exception/     — перенесено из КР1 без изменений
        └── util/          — DatabaseManager и ExcelExporter без изменений
```

## Запуск

Эта папка кладётся **рядом** с существующими файлами КР1 в том же git-репозитории
(как сиблинг, а не вместо них):

```
ComputerService/           (корень репозитория)
├── pom.xml                — это КР1 (console-app), не трогаем
├── db/
├── src/
└── javafx-client/         — вот эта новая папка (КР2)
    ├── pom.xml
    └── src/
```

### 1. БД уже должна быть поднята (из КР1)

Если контейнер с PostgreSQL из КР1 остановлен — запустите его заново:
```bash
docker start pg-service
```

### 2. Открыть модуль в IntelliJ IDEA

Откройте папку `javafx-client` как отдельный Maven-проект (File → Open →
выбрать `javafx-client/pom.xml` → Open as Project). IntelliJ IDEA сама подтянет
зависимости JavaFX из `pom.xml`.

### 3. Проверить database.properties

Укажите те же параметры, что использовались в КР1 (`src/main/resources/database.properties`).

### 4. Собрать и запустить

```bash
cd javafx-client
mvn clean compile
mvn javafx:run
```

Либо через IntelliJ IDEA: открыть `ComputerServiceFxApp.java` → кнопка Run.

## Что реализовано (чек-лист ТЗ КР2)

- [x] Главное окно: список заявок, добавление/редактирование/удаление, поиск, фильтрация,
      сортировка, обновление, переход к статистике, экспорт — всё в одном окне
- [x] `TableView` для основной сущности (заявки) с автоматическим обновлением после CRUD
- [x] Полный CRUD через GUI-формы, а не консоль
- [x] Подтверждение перед удалением (диалог Alert)
- [x] Выбор клиента через `ComboBox<Client>` — без ручного ввода ID (связанная сущность)
- [x] Валидация данных до сохранения, ошибки показываются прямо в форме, а не в консоли
- [x] Диалоговые окна для добавления/редактирования/удаления/ошибок
- [x] 2+ способа поиска (по описанию и по клиенту — объединены в одном поле),
      2+ фильтра (статус, приоритет), 2+ сортировки (по дате, по стоимости)
- [x] Статистика — 6 показателей (как и в КР1)
- [x] Экспорт в `.xlsx` через `FileChooser` (путь выбирает пользователь)
- [x] Интерфейс вынесен в FXML, оформление — в CSS
- [x] Логика разделена: View/FXML → Controller → Service → Repository/DAO → БД
- [x] Собственное JDBC-подключение к БД из КР1 (не через REST API)

## Что доделать перед сдачей

- [ ] Подставить реальные параметры БД в `database.properties`
- [ ] Прогнать у всех участников команды `mvn clean compile` и `mvn javafx:run`
- [ ] Проверить все пункты меню вручную ещё раз перед защитой
- [ ] Подготовиться объяснить: `Stage`/`Scene`/`Node`, связь FXML ↔ Controller,
      зачем `ObservableList`, где бизнес-логика, как обрабатываются исключения
