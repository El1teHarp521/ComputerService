package ru.mirea.computerservice.fx.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.fx.util.FxTasks;
import ru.mirea.computerservice.fx.util.Ui;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.repository.ClientRepositoryJdbc;
import ru.mirea.computerservice.repository.RepairRequestRepositoryJdbc;
import ru.mirea.computerservice.service.ClientService;
import ru.mirea.computerservice.service.RepairRequestService;
import ru.mirea.computerservice.service.RequestSortOrder;
import ru.mirea.computerservice.util.ExcelExporter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Контроллер главного окна. Это слой «Controller» из архитектуры
 * View/FXML -> Controller -> Service -> Repository/DAO -> БД:
 * он принимает действия пользователя, вызывает сервисы и обновляет таблицы.
 * SQL и бизнес-правил здесь нет — они в *RepositoryJdbc и *Service.
 *
 * Связь с FXML: поля с @FXML получают объекты разметки по совпадению имени с атрибутом fx:id,
 * а методы-обработчики подключены атрибутами onAction="#имяМетода" в MainView.fxml.
 */
public class MainController {

    private static final String FXML_DIR = "/ru/mirea/computerservice/fx/";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Locale RU = Locale.forLanguageTag("ru");

    // ---------- общие элементы окна ----------
    @FXML private TabPane mainTabs;
    @FXML private Tab requestsTab;
    @FXML private Tab statsTab;
    @FXML private ProgressBar busyBar;
    @FXML private Label statusLabel;

    // ---------- поиск, фильтры, сортировка ----------
    @FXML private HBox searchRow;
    @FXML private HBox filterGroup;
    @FXML private TextField searchField;
    @FXML private ComboBox<RequestSortOrder> sortCombo;
    @FXML private ComboBox<RequestStatus> statusFilterCombo;
    @FXML private ComboBox<Priority> priorityFilterCombo;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;

    // ---------- вкладка «Заявки» ----------
    @FXML private TableView<RepairRequest> requestsTable;
    @FXML private TableColumn<RepairRequest, Integer> colId;
    @FXML private TableColumn<RepairRequest, String> colClient;
    @FXML private TableColumn<RepairRequest, String> colDevice;
    @FXML private TableColumn<RepairRequest, String> colDescription;
    @FXML private TableColumn<RepairRequest, String> colStatus;
    @FXML private TableColumn<RepairRequest, String> colPriority;
    @FXML private TableColumn<RepairRequest, BigDecimal> colCost;
    @FXML private TableColumn<RepairRequest, LocalDateTime> colCreatedAt;
    @FXML private Label requestsStatusLabel;
    @FXML private Button editRequestButton;
    @FXML private Button deleteRequestButton;

    // панель «Сведения о заявке»
    @FXML private Label detailTitle;
    @FXML private Label detailClient;
    @FXML private Label detailContacts;
    @FXML private Label detailDevice;
    @FXML private Label detailStatus;
    @FXML private Label detailPriority;
    @FXML private Label detailCost;
    @FXML private Label detailCreated;
    @FXML private Label detailCompleted;
    @FXML private TextArea detailDescription;

    // ---------- вкладка «Клиенты» ----------
    @FXML private TableView<Client> clientsTable;
    @FXML private TableColumn<Client, Integer> colClientId;
    @FXML private TableColumn<Client, String> colClientName;
    @FXML private TableColumn<Client, String> colClientPhone;
    @FXML private TableColumn<Client, String> colClientEmail;
    @FXML private Button editClientButton;
    @FXML private Button deleteClientButton;

    // связанные данные: заявки выбранного клиента
    @FXML private Label clientRequestsTitle;
    @FXML private TableView<RepairRequest> clientRequestsTable;
    @FXML private TableColumn<RepairRequest, Integer> ccId;
    @FXML private TableColumn<RepairRequest, String> ccDevice;
    @FXML private TableColumn<RepairRequest, String> ccDescription;
    @FXML private TableColumn<RepairRequest, String> ccStatus;
    @FXML private TableColumn<RepairRequest, String> ccPriority;
    @FXML private TableColumn<RepairRequest, BigDecimal> ccCost;
    @FXML private TableColumn<RepairRequest, LocalDateTime> ccCreatedAt;

    // ---------- вкладка «Статистика» ----------
    @FXML private GridPane statsGrid;
    @FXML private BarChart<String, Number> statusChart;

    // Тот же слой Service/Repository, что и в консольной КР1; отличается только UI.
    private final ClientService clientService = new ClientService(new ClientRepositoryJdbc());
    private final RepairRequestService requestService =
            new RepairRequestService(new RepairRequestRepositoryJdbc(), clientService);

    // ObservableList — «наблюдаемый» список: TableView подписана на него и перерисовывается сама,
    // когда в списке меняются элементы (setAll / add / remove).
    private final ObservableList<RepairRequest> requestsData = FXCollections.observableArrayList();
    private final ObservableList<Client> clientsData = FXCollections.observableArrayList();
    private final ObservableList<RepairRequest> clientRequestsData = FXCollections.observableArrayList();

    private int busyCount;          // сколько фоновых операций сейчас выполняется
    private int requestsLoadSeq;    // номер последней загрузки заявок (защита от «устаревших» ответов)
    private boolean suppressReload; // true, пока сброс фильтров меняет сразу несколько контролов

    private record StatsSnapshot(Map<String, String> indicators, Map<RequestStatus, Long> byStatus) {
    }

    // ===================================================================== инициализация

    /** Вызывается FXMLLoader-ом сразу после загрузки разметки и внедрения всех @FXML-полей. */
    @FXML
    private void initialize() {
        setupRequestsTable();
        setupClientRequestsTable();
        setupClientsTable();
        setupFilters();
        setupSelectionHandling();
        refreshAll();
    }

    private void setupRequestsTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        setupCommonRequestColumns(colDevice, colDescription, colStatus, colPriority, colCost, colCreatedAt);
        requestsTable.setItems(requestsData);
        stretchColumn(requestsTable, colDescription);
        requestsTable.setPlaceholder(new Label("Заявок нет или они не подходят под условия поиска"));

        // раскраска строк по состоянию заявки + двойной щелчок = редактирование
        requestsTable.setRowFactory(table -> {
            TableRow<RepairRequest> row = new TableRow<>() {
                @Override
                protected void updateItem(RepairRequest item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("row-done", "row-cancelled", "row-urgent");
                    if (!empty && item != null) {
                        if (item.getStatus() == RequestStatus.DONE) {
                            getStyleClass().add("row-done");
                        } else if (item.getStatus() == RequestStatus.CANCELLED) {
                            getStyleClass().add("row-cancelled");
                        } else if (item.getPriority() == Priority.URGENT) {
                            getStyleClass().add("row-urgent");
                        }
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEditRequest();
                }
            });
            return row;
        });
    }

    private void setupClientRequestsTable() {
        ccId.setCellValueFactory(new PropertyValueFactory<>("id"));
        setupCommonRequestColumns(ccDevice, ccDescription, ccStatus, ccPriority, ccCost, ccCreatedAt);
        clientRequestsTable.setItems(clientRequestsData);
        stretchColumn(clientRequestsTable, ccDescription);
        clientRequestsTable.setPlaceholder(new Label("У выбранного клиента нет заявок"));
    }

    /**
     * Колонки, одинаковые для обеих таблиц заявок (общей и «заявки клиента»).
     * Вынесено в метод, чтобы не дублировать настройку — в ТЗ это прямо запрещено.
     */
    private void setupCommonRequestColumns(TableColumn<RepairRequest, String> device,
                                           TableColumn<RepairRequest, String> description,
                                           TableColumn<RepairRequest, String> status,
                                           TableColumn<RepairRequest, String> priority,
                                           TableColumn<RepairRequest, BigDecimal> cost,
                                           TableColumn<RepairRequest, LocalDateTime> createdAt) {
        device.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        description.setCellValueFactory(new PropertyValueFactory<>("problemDescription"));
        // enum показываем по-русски; сортировка по этой колонке идёт по тексту названия
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        priority.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority().getDisplayName()));

        // Значения колонок «стоимость» и «дата» остаются числом/датой (поэтому сортируются верно),
        // а красиво форматируются только при отрисовке ячейки.
        cost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        cost.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format(RU, "%,.2f", item));
            }
        });
        createdAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAt.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DATE_TIME));
            }
        });
    }

    private void setupClientsTable() {
        colClientId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClientName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colClientPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colClientEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        clientsTable.setItems(clientsData);
        stretchColumn(clientsTable, colClientName);
        clientsTable.setPlaceholder(new Label("Клиентов пока нет"));
        clientsTable.setRowFactory(table -> {
            TableRow<Client> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEditClient();
                }
            });
            return row;
        });
    }

    private void setupFilters() {
        sortCombo.setItems(FXCollections.observableArrayList(RequestSortOrder.values()));
        sortCombo.setValue(RequestSortOrder.BY_ID);
        configureFilterCombo(statusFilterCombo, RequestStatus.values(), "Все статусы");
        configureFilterCombo(priorityFilterCombo, Priority.values(), "Любой приоритет");

        // Любое изменение условия сразу перезапрашивает таблицу (кроме массового сброса).
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> reloadIfAllowed());
        statusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> reloadIfAllowed());
        priorityFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> reloadIfAllowed());
        dateFromPicker.valueProperty().addListener((obs, oldValue, newValue) -> reloadIfAllowed());
        dateToPicker.valueProperty().addListener((obs, oldValue, newValue) -> reloadIfAllowed());
    }

    /**
     * Фильтр-ComboBox с первым пунктом-«заглушкой» (null) вида «Все статусы»:
     * выбор этого пункта снимает фильтр. Подпись показывается и в закрытом списке.
     */
    private static <T> void configureFilterCombo(ComboBox<T> combo, T[] values, String allLabel) {
        ObservableList<T> items = FXCollections.observableArrayList();
        items.add(null);
        items.addAll(values);
        combo.setItems(items);
        combo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? allLabel : item.toString()));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? allLabel : item.toString());
            }
        });
    }

    private void setupSelectionHandling() {
        // кнопки «Изменить/Удалить» доступны только при выбранной строке
        editRequestButton.disableProperty().bind(requestsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteRequestButton.disableProperty().bind(requestsTable.getSelectionModel().selectedItemProperty().isNull());
        editClientButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteClientButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull());

        requestsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showRequestDetails(newValue));
        clientsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> loadClientRequests(newValue));
        // Условия поиска относятся только к заявкам: на других вкладках они затемнены
        searchRow.disableProperty().bind(requestsTab.selectedProperty().not());
        filterGroup.disableProperty().bind(requestsTab.selectedProperty().not());
        showRequestDetails(null);
    }

    /**
     * Ширина колонки = всё свободное место таблицы. Нужна вместо CONSTRAINED_RESIZE_POLICY,
     * который игнорирует заданные в FXML ширины и делает все колонки одинаковыми:
     * у остальных колонок остаётся заданная ширина, а «длинная» колонка занимает остаток.
     */
    private static <S> void stretchColumn(TableView<S> table, TableColumn<S, ?> stretch) {
        Observable[] others = table.getColumns().stream()
                .filter(column -> column != stretch)
                .map(TableColumn::widthProperty)
                .toArray(Observable[]::new);
        stretch.setMinWidth(120);
        stretch.prefWidthProperty().bind(table.widthProperty().subtract(
                Bindings.createDoubleBinding(() -> table.getColumns().stream()
                        .filter(column -> column != stretch)
                        .mapToDouble(TableColumn::getWidth).sum(), others)).subtract(18));
    }

    // ===================================================================== загрузка данных

    /** Перечитывает из базы всё, что видно на экране. Вызывается после любого изменения данных. */
    private void refreshAll() {
        loadClients();
        loadRequests();
        loadStatistics();
    }

    private void reloadIfAllowed() {
        if (!suppressReload) {
            loadRequests();
        }
    }

    private void loadClients() {
        Integer selectedId = selectedId(clientsTable, Client::getId);
        async(clientService::getAll, clients -> {
            clientsData.setAll(clients);
            reselect(clientsTable, Client::getId, selectedId);
            showRequestDetails(requestsTable.getSelectionModel().getSelectedItem()); // обновить контакты
        }, this::fail);
    }

    private void loadRequests() {
        // Условия читаются здесь, в UI-потоке; в фоновый поток уходят уже готовые значения.
        String keyword = searchField.getText();
        RequestStatus status = statusFilterCombo.getValue();
        Priority priority = priorityFilterCombo.getValue();
        LocalDate from = dateFromPicker.getValue();
        LocalDate to = dateToPicker.getValue();
        RequestSortOrder order = sortCombo.getValue();
        Integer selectedId = selectedId(requestsTable, RepairRequest::getId);
        int seq = ++requestsLoadSeq;

        async(() -> requestService.findFiltered(keyword, status, priority, from, to, order),
                list -> {
                    if (seq != requestsLoadSeq) {
                        return; // пока шёл этот запрос, уже запущен более новый — результат устарел
                    }
                    requestsData.setAll(list);
                    reselect(requestsTable, RepairRequest::getId, selectedId);
                    requestsStatusLabel.getStyleClass().remove("error-label");
                    boolean filtered = (keyword != null && !keyword.isBlank())
                            || status != null || priority != null || from != null || to != null;
                    requestsStatusLabel.setText("Показано заявок: " + list.size() + (filtered ? " (применены условия)" : ""));
                },
                error -> {
                    if (seq != requestsLoadSeq) {
                        return;
                    }
                    if (error instanceof BusinessException) { // например, неверный период дат — без всплывающего окна
                        if (!requestsStatusLabel.getStyleClass().contains("error-label")) {
                            requestsStatusLabel.getStyleClass().add("error-label");
                        }
                        requestsStatusLabel.setText(error.getMessage());
                    } else {
                        fail(error);
                    }
                });
    }

    private void loadClientRequests(Client client) {
        clientRequestsData.clear();
        if (client == null) {
            clientRequestsTitle.setText("Заявки выбранного клиента");
            return;
        }
        int clientId = client.getId();
        clientRequestsTitle.setText("Заявки клиента: " + client.getFullName());
        async(() -> requestService.getByClient(clientId), list -> {
            Client current = clientsTable.getSelectionModel().getSelectedItem();
            if (current != null && current.getId() == clientId) { // выбор мог уже смениться
                clientRequestsData.setAll(list);
                clientRequestsTitle.setText("Заявки клиента: " + current.getFullName() + " (" + list.size() + ")");
            }
        }, this::fail);
    }

    private void loadStatistics() {
        async(() -> new StatsSnapshot(requestService.getStatistics(), requestService.countByStatus()),
                this::renderStatistics, this::fail);
    }

    private void renderStatistics(StatsSnapshot snapshot) {
        statsGrid.getChildren().clear();
        int row = 0;
        for (Map.Entry<String, String> entry : snapshot.indicators().entrySet()) {
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("stat-name");
            Label value = new Label(entry.getValue());
            value.getStyleClass().add("stat-value");
            statsGrid.add(name, 0, row);
            statsGrid.add(value, 1, row);
            row++;
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        snapshot.byStatus().forEach((status, count) ->
                series.getData().add(new XYChart.Data<>(status.getDisplayName(), count)));
        statusChart.getData().setAll(List.of(series));

        // Количество заявок — целое число, поэтому ось с шагом 1 и без дробных делений
        long maxCount = snapshot.byStatus().values().stream().mapToLong(Long::longValue).max().orElse(0);
        NumberAxis yAxis = (NumberAxis) statusChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(Math.max(1, maxCount) + 1);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
    }

    // ===================================================================== заявки: CRUD

    @FXML
    private void onAddRequest() {
        openRequestDialog(null);
    }

    @FXML
    private void onEditRequest() {
        RepairRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openRequestDialog(selected);
        }
    }

    @FXML
    private void onDeleteRequest() {
        RepairRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        // Обязательное подтверждение перед удалением (требование ТЗ)
        if (!Ui.confirm(owner(), "Удалить заявку №" + selected.getId() + " (" + selected.getDeviceType()
                + ", клиент: " + selected.getClientName() + ")?\nДействие нельзя отменить.")) {
            return;
        }
        async(() -> {
            requestService.delete(selected.getId());
            return null;
        }, result -> {
            statusLabel.setText("Заявка №" + selected.getId() + " удалена");
            refreshAll();
        }, this::fail);
    }

    private void openRequestDialog(RepairRequest toEdit) {
        if (clientsData.isEmpty()) {
            Ui.warning(owner(), "Сначала добавьте хотя бы одного клиента на вкладке «Клиенты».");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_DIR + "RequestFormDialog.fxml"));
            Parent root = loader.load();
            RequestFormDialogController controller = loader.getController();

            Stage stage = createDialogStage(root, toEdit == null ? "Новая заявка" : "Заявка №" + toEdit.getId());
            controller.setDialogStage(stage);
            controller.setRequestService(requestService);
            controller.initLookups(new ArrayList<>(clientsData));
            if (toEdit != null) {
                controller.initForEdit(toEdit);
            }

            stage.showAndWait(); // блокирует главное окно, пока форма открыта
            if (controller.isSaved()) {
                statusLabel.setText(toEdit == null ? "Заявка создана" : "Заявка №" + toEdit.getId() + " сохранена");
                refreshAll();
            }
        } catch (IOException e) {
            Ui.error(owner(), "Не удалось открыть форму заявки: " + e.getMessage());
        }
    }

    // ===================================================================== клиенты: CRUD

    @FXML
    private void onAddClient() {
        openClientDialog(null);
    }

    @FXML
    private void onEditClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openClientDialog(selected);
        }
    }

    @FXML
    private void onDeleteClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!Ui.confirm(owner(), "Удалить клиента «" + selected.getFullName() + "»?")) {
            return;
        }
        // Бизнес-правила удаления (нет активных заявок, нет истории) проверяет ClientService
        async(() -> {
            clientService.delete(selected.getId());
            return null;
        }, result -> {
            statusLabel.setText("Клиент «" + selected.getFullName() + "» удалён");
            refreshAll();
        }, this::fail);
    }

    private void openClientDialog(Client toEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_DIR + "ClientFormDialog.fxml"));
            Parent root = loader.load();
            ClientFormDialogController controller = loader.getController();

            Stage stage = createDialogStage(root, toEdit == null ? "Новый клиент" : "Изменение клиента");
            controller.setDialogStage(stage);
            controller.setClientService(clientService);
            if (toEdit != null) {
                controller.initForEdit(toEdit);
            }

            stage.showAndWait();
            if (controller.isSaved()) {
                statusLabel.setText(toEdit == null ? "Клиент добавлен" : "Данные клиента сохранены");
                refreshAll();
            }
        } catch (IOException e) {
            Ui.error(owner(), "Не удалось открыть форму клиента: " + e.getMessage());
        }
    }

    private Stage createDialogStage(Parent root, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initOwner(owner());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        return stage;
    }

    // ===================================================================== поиск и фильтры

    @FXML
    private void onSearch() {
        loadRequests();
    }

    @FXML
    private void onResetFilter() {
        suppressReload = true; // чтобы пять изменений ниже дали ОДИН запрос, а не пять
        searchField.clear();
        statusFilterCombo.setValue(null);
        priorityFilterCombo.setValue(null);
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        sortCombo.setValue(RequestSortOrder.BY_ID);
        suppressReload = false;
        loadRequests();
    }

    @FXML
    private void onRefresh() {
        refreshAll();
        statusLabel.setText("Данные обновлены");
    }

    // ===================================================================== статистика, экспорт

    @FXML
    private void onShowStatistics() {
        mainTabs.getSelectionModel().select(statsTab);
        loadStatistics();
    }

    /** Экспортирует ТЕКУЩИЙ набор данных — ровно те строки и в том порядке, что видны в таблице. */
    @FXML
    private void onExport() {
        List<RepairRequest> rows = new ArrayList<>(requestsTable.getItems());
        if (rows.isEmpty()) {
            Ui.warning(owner(), "В таблице нет заявок — экспортировать нечего.");
            return;
        }
        // Путь выбирает пользователь системным диалогом; в коде нет ни одного жёстко заданного пути.
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить заявки в Excel");
        chooser.setInitialFileName("repair_requests_" + LocalDate.now() + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File chosen = chooser.showSaveDialog(owner());
        if (chosen == null) {
            return; // нажали «Отмена»
        }
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen
                : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");

        async(() -> {
            ExcelExporter.exportRequests(rows, target.getAbsolutePath());
            return rows.size();
        }, count -> {
            statusLabel.setText("Экспортировано заявок: " + count);
            Ui.info(owner(), "Экспортировано заявок: " + count + "\nФайл: " + target.getAbsolutePath());
        }, this::fail);
    }

    // ===================================================================== сведения о заявке

    private void showRequestDetails(RepairRequest request) {
        if (request == null) {
            detailTitle.setText("Выберите заявку в таблице");
            for (Label label : List.of(detailClient, detailContacts, detailDevice, detailStatus,
                    detailPriority, detailCost, detailCreated, detailCompleted)) {
                label.setText("—");
            }
            detailDescription.clear();
            return;
        }
        // Связанная сущность: контакты клиента берутся из уже загруженного списка клиентов
        Client client = clientsData.stream().filter(c -> c.getId() == request.getClientId()).findFirst().orElse(null);
        detailTitle.setText("Заявка №" + request.getId());
        detailClient.setText(request.getClientName());
        detailContacts.setText(client == null ? "—"
                : client.getPhone() + (client.getEmail() == null ? "" : "\n" + client.getEmail()));
        detailDevice.setText(request.getDeviceType());
        detailStatus.setText(request.getStatus().getDisplayName());
        detailPriority.setText(request.getPriority().getDisplayName());
        detailCost.setText(String.format(RU, "%,.2f ₽", request.getCost()));
        detailCreated.setText(request.getCreatedAt() == null ? "—" : request.getCreatedAt().format(DATE_TIME));
        detailCompleted.setText(request.getCompletedAt() == null ? "—" : request.getCompletedAt().format(DATE_TIME));
        detailDescription.setText(request.getProblemDescription());
    }

    // ===================================================================== меню

    @FXML
    private void onAbout() {
        Ui.info(owner(), "Система компьютерного сервиса  (JavaFX).\n"
                + "Работает с той же базой PostgreSQL, что и консольное приложение КР1,\n"
                + "через собственное JDBC-подключение (без REST API).");
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    // ===================================================================== вспомогательное

    /**
     * Единый способ выполнить работу с БД/файлом в фоне: показывает индикатор занятости,
     * по завершении возвращает результат в UI-поток. Счётчик нужен, потому что операций
     * может быть несколько одновременно (например, три загрузки при обновлении).
     */
    private <T> void async(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        busyCount++;
        busyBar.setVisible(true);
        FxTasks.run(work,
                result -> {
                    finishBusy();
                    onSuccess.accept(result);
                },
                error -> {
                    finishBusy();
                    onFailure.accept(error);
                });
    }

    private void finishBusy() {
        busyCount--;
        busyBar.setVisible(busyCount > 0);
    }

    private void fail(Throwable error) {
        statusLabel.setText("Ошибка: операция не выполнена");
        Ui.error(owner(), error);
    }

    private Window owner() {
        return requestsTable.getScene() == null ? null : requestsTable.getScene().getWindow();
    }

    private static <T> Integer selectedId(TableView<T> table, ToIntFunction<T> idOf) {
        T selected = table.getSelectionModel().getSelectedItem();
        return selected == null ? null : idOf.applyAsInt(selected);
    }

    /** После перезагрузки списка возвращает выделение на ту же запись (по id), если она осталась. */
    private static <T> void reselect(TableView<T> table, ToIntFunction<T> idOf, Integer id) {
        if (id == null) {
            return;
        }
        for (T item : table.getItems()) {
            if (idOf.applyAsInt(item) == id) {
                table.getSelectionModel().select(item);
                table.scrollTo(item);
                return;
            }
        }
    }
}
