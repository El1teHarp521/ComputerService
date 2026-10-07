package ru.mirea.computerservice.fx.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Контроллер главного окна — это ровно слой "Console UI" из архитектуры КР1,
 * только теперь он не печатает текст в консоль, а обновляет таблицы и открывает
 * диалоговые окна. SQL и бизнес-правила сюда НЕ попадают — только вызовы
 * ClientService / RepairRequestService, в точности как в Main.java из КР1.
 */
public class MainController {

    // ---- Колонки и элементы вкладки "Заявки" ----
    @FXML private TableView<RepairRequest> requestsTable;
    @FXML private TableColumn<RepairRequest, Integer> colId;
    @FXML private TableColumn<RepairRequest, String> colClient;
    @FXML private TableColumn<RepairRequest, String> colDevice;
    @FXML private TableColumn<RepairRequest, String> colDescription;
    @FXML private TableColumn<RepairRequest, String> colStatus;
    @FXML private TableColumn<RepairRequest, String> colPriority;
    @FXML private TableColumn<RepairRequest, BigDecimal> colCost;
    @FXML private TableColumn<RepairRequest, String> colCreatedAt;
    @FXML private Label requestsStatusLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<RequestStatus> statusFilterCombo;
    @FXML private ComboBox<Priority> priorityFilterCombo;

    // ---- Колонки и элементы вкладки "Клиенты" ----
    @FXML private TableView<Client> clientsTable;
    @FXML private TableColumn<Client, Integer> colClientId;
    @FXML private TableColumn<Client, String> colClientName;
    @FXML private TableColumn<Client, String> colClientPhone;
    @FXML private TableColumn<Client, String> colClientEmail;

    // Тот же сервисный слой, что и в консольной КР1 — отличается только то,
    // что вызывает его теперь JavaFX, а не Scanner с консольным меню.
    private final ClientRepository clientRepository = new ClientRepositoryJdbc();
    private final ClientService clientService = new ClientService(clientRepository);
    private final RepairRequestRepository requestRepository = new RepairRequestRepositoryJdbc();
    private final RepairRequestService requestService =
            new RepairRequestService(requestRepository, clientService);

    // ObservableList — специальная JavaFX-коллекция: TableView сама перерисовывается,
    // когда её содержимое меняется через setAll()/add()/remove()
    private final ObservableList<RepairRequest> requestsData = FXCollections.observableArrayList();
    private final ObservableList<Client> clientsData = FXCollections.observableArrayList();

    /**
     * Вызывается автоматически FXMLLoader-ом сразу после того, как все @FXML-поля
     * уже внедрены (аннотация @FXML на самом методе — специальный "метод жизненного цикла").
     */
    @FXML
    public void initialize() {
        setupRequestsTable();
        setupClientsTable();
        setupFilters();
        reloadAll();
    }

    // ---------------- Настройка таблиц ----------------

    private void setupRequestsTable() {
        // PropertyValueFactory находит метод getId()/getDeviceType()/... по имени поля через рефлексию
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("problemDescription"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));

        // Статус и приоритет — enum-ы, а в таблице должно быть русское название,
        // поэтому здесь не PropertyValueFactory, а своя лямбда с SimpleStringProperty
        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));
        colPriority.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPriority().getDisplayName()));
        colCreatedAt.setCellValueFactory(cellData -> {
            var createdAt = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "" : createdAt.toLocalDate().toString());
        });

        requestsTable.setItems(requestsData);
    }

    private void setupClientsTable() {
        colClientId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClientName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colClientPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colClientEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        clientsTable.setItems(clientsData);
    }

    private void setupFilters() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(RequestStatus.values()));
        priorityFilterCombo.setItems(FXCollections.observableArrayList(Priority.values()));

        // Фильтр применяется сразу при выборе значения в списке — отдельную кнопку "Применить" не делаем
        statusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                requestsData.setAll(requestService.filterByStatus(newValue));
                requestsStatusLabel.setText("По статусу \"" + newValue.getDisplayName() + "\": " + requestsData.size());
            }
        });
        priorityFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                requestsData.setAll(requestService.filterByPriority(newValue));
                requestsStatusLabel.setText("По приоритету \"" + newValue.getDisplayName() + "\": " + requestsData.size());
            }
        });
    }

    /** Перечитывает обе таблицы из базы данных заново. Вызывается после любого CRUD-действия. */
    private void reloadAll() {
        try {
            requestsData.setAll(requestService.getAll());
            clientsData.setAll(clientService.getAll());
            requestsStatusLabel.setText("Записей: " + requestsData.size());
        } catch (DatabaseException e) {
            showError("Ошибка базы данных", e.getMessage());
        }
    }

    // ---------------- Заявки: CRUD ----------------

    @FXML
    private void onAddRequest() {
        openRequestDialog(null);
    }

    @FXML
    private void onEditRequest() {
        RepairRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите заявку в таблице.");
            return;
        }
        openRequestDialog(selected);
    }

    @FXML
    private void onDeleteRequest() {
        RepairRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите заявку в таблице.");
            return;
        }
        // Обязательное подтверждение перед удалением — требование ТЗ
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить заявку №" + selected.getId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                try {
                    requestService.delete(selected.getId());
                    reloadAll();
                } catch (BusinessException | EntityNotFoundException e) {
                    showError("Нельзя удалить", e.getMessage());
                } catch (DatabaseException e) {
                    showError("Ошибка базы данных", e.getMessage());
                }
            }
        });
    }

    /**
     * Общий метод открытия формы заявки: requestToEdit == null -> режим создания,
     * иначе -> режим редактирования. Используется и onAddRequest(), и onEditRequest().
     */
    private void openRequestDialog(RepairRequest requestToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ru/mirea/computerservice/fx/RequestFormDialog.fxml"));
            Parent root = loader.load();

            RequestFormDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle(requestToEdit == null ? "Новая заявка" : "Изменение заявки");
            dialogStage.initModality(Modality.APPLICATION_MODAL); // блокирует главное окно, пока форма открыта
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);
            controller.setRequestService(requestService);
            controller.initLookups(clientService.getAll());
            if (requestToEdit != null) {
                controller.initForEdit(requestToEdit);
            }

            dialogStage.showAndWait(); // ждём, пока пользователь закроет форму
            if (controller.isSaved()) {
                reloadAll();
            }
        } catch (IOException e) {
            showError("Ошибка интерфейса", "Не удалось открыть форму заявки: " + e.getMessage());
        }
    }

    // ---------------- Клиенты: CRUD ----------------

    @FXML
    private void onAddClient() {
        openClientDialog(null);
    }

    @FXML
    private void onEditClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите клиента в таблице.");
            return;
        }
        openClientDialog(selected);
    }

    @FXML
    private void onDeleteClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите клиента в таблице.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить клиента \"" + selected.getFullName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                try {
                    // ClientService сам проверит бизнес-правило "нельзя удалить клиента
                    // с активными заявками" и бросит BusinessException, если нарушено
                    clientService.delete(selected.getId());
                    reloadAll();
                } catch (BusinessException | EntityNotFoundException e) {
                    showError("Нельзя удалить", e.getMessage());
                } catch (DatabaseException e) {
                    showError("Ошибка базы данных", e.getMessage());
                }
            }
        });
    }

    private void openClientDialog(Client clientToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ru/mirea/computerservice/fx/ClientFormDialog.fxml"));
            Parent root = loader.load();

            ClientFormDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle(clientToEdit == null ? "Новый клиент" : "Изменение клиента");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);
            controller.setClientService(clientService);
            if (clientToEdit != null) {
                controller.initForEdit(clientToEdit);
            }

            dialogStage.showAndWait();
            if (controller.isSaved()) {
                reloadAll();
            }
        } catch (IOException e) {
            showError("Ошибка интерфейса", "Не удалось открыть форму клиента: " + e.getMessage());
        }
    }

    // ---------------- Поиск, фильтрация, сортировка ----------------

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            reloadAll();
            return;
        }
        try {
            // ТЗ требует минимум 2 способа поиска — по описанию и по имени клиента.
            // Одно поле ввода покрывает оба сразу: ищем обоими способами и объединяем
            // результаты в LinkedHashSet, чтобы не было повторов и сохранялся порядок.
            List<RepairRequest> byDescription = requestService.searchByDescription(keyword);
            List<RepairRequest> byClient = requestService.searchByClientName(keyword);
            LinkedHashSet<RepairRequest> merged = new LinkedHashSet<>(byDescription);
            merged.addAll(byClient);

            requestsData.setAll(merged);
            requestsStatusLabel.setText("Найдено: " + requestsData.size());
        } catch (DatabaseException e) {
            showError("Ошибка базы данных", e.getMessage());
        }
    }

    @FXML
    private void onResetFilter() {
        statusFilterCombo.getSelectionModel().clearSelection();
        priorityFilterCombo.getSelectionModel().clearSelection();
        searchField.clear();
        reloadAll();
    }

    @FXML
    private void onSortByDate() {
        requestsData.setAll(requestService.sortByDate());
    }

    @FXML
    private void onSortByCost() {
        requestsData.setAll(requestService.sortByCost());
    }

    @FXML
    private void onRefresh() {
        reloadAll();
    }

    // ---------------- Статистика ----------------

    @FXML
    private void onStatistics() {
        Map<String, Object> stats = requestService.getStatistics();
        StringBuilder text = new StringBuilder();
        stats.forEach((key, value) -> text.append(key).append(": ").append(value).append("\n"));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Статистика");
        alert.setHeaderText("Статистика системы");
        alert.setContentText(text.toString());
        alert.showAndWait();
    }

    // ---------------- Экспорт ----------------

    @FXML
    private void onExport() {
        // Путь к файлу выбирает пользователь через системный диалог,
        // никаких жёстко прописанных путей к файлам на диске — требование ТЗ
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчёт как...");
        fileChooser.setInitialFileName("repair_requests.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel файлы (*.xlsx)", "*.xlsx"));

        File file = fileChooser.showSaveDialog(requestsTable.getScene().getWindow());
        if (file == null) {
            return; // пользователь нажал "Отмена" в системном диалоге
        }
        try {
            ExcelExporter.exportRequests(requestService.getAll(), file.getAbsolutePath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Экспорт выполнен: " + file.getName());
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (IOException e) {
            showError("Ошибка экспорта", e.getMessage());
        }
    }

    // ---------------- Меню "Справка" и "Выход" ----------------

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Система компьютерного сервиса — КР2 (JavaFX-клиент).\n"
                        + "Работает с той же базой данных PostgreSQL, что и консольное приложение КР1.",
                ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    // ---------------- Вспомогательные диалоги ----------------

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
