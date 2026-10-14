package ru.mirea.computerservice.fx.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.computerservice.fx.util.FxTasks;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.service.RepairRequestService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Контроллер окна «Заявка на ремонт». Клиент выбирается из выпадающего списка существующих
 * клиентов (ComboBox), а не вводится числом-ID — это требование ТЗ про связанные сущности.
 */
public class RequestFormDialogController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<Client> clientCombo;
    @FXML private TextField deviceTypeField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Priority> priorityCombo;
    @FXML private ComboBox<RequestStatus> statusCombo;
    @FXML private TextField costField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private RepairRequestService requestService;
    private Stage dialogStage;
    private RepairRequest editingRequest; // null => режим создания
    private boolean saved;

    @FXML
    private void initialize() {
        saveButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);
    }

    public void setRequestService(RepairRequestService requestService) {
        this.requestService = requestService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Заполняет выпадающие списки; вызывается до показа окна. */
    public void initLookups(List<Client> clients) {
        clientCombo.setItems(FXCollections.observableArrayList(clients));
        priorityCombo.setItems(FXCollections.observableArrayList(Priority.values()));
        statusCombo.setItems(FXCollections.observableArrayList(RequestStatus.values()));

        priorityCombo.getSelectionModel().select(Priority.NORMAL);
        statusCombo.getSelectionModel().select(RequestStatus.NEW);
        statusCombo.setDisable(true); // новая заявка всегда создаётся со статусом «Принята»
        costField.setText("0");
    }

    /** Переключает форму в режим редактирования существующей заявки. */
    public void initForEdit(RepairRequest request) {
        this.editingRequest = request;
        titleLabel.setText("Изменить заявку №" + request.getId());

        for (Client client : clientCombo.getItems()) {
            if (client.getId() == request.getClientId()) {
                clientCombo.getSelectionModel().select(client);
                break;
            }
        }
        clientCombo.setDisable(true); // заявку нельзя «перевесить» на другого клиента

        deviceTypeField.setText(request.getDeviceType());
        descriptionArea.setText(request.getProblemDescription());
        priorityCombo.getSelectionModel().select(request.getPriority());
        statusCombo.getSelectionModel().select(request.getStatus());
        statusCombo.setDisable(false);
        costField.setText(request.getCost() == null ? "0" : request.getCost().toPlainString());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        hideError();

        Client client = clientCombo.getValue();
        if (client == null) {
            showError("Клиент: выберите клиента из списка");
            return;
        }
        BigDecimal cost;
        try {
            cost = parseCost(costField.getText());
        } catch (NumberFormatException e) {
            showError("Стоимость: введите число, например 1500 или 1500.50");
            return;
        }

        String deviceType = deviceTypeField.getText();
        String description = descriptionArea.getText();
        Priority priority = priorityCombo.getValue();
        RequestStatus status = statusCombo.getValue();
        RepairRequest editing = editingRequest;

        saveButton.setDisable(true);
        FxTasks.run(() -> {
                    if (editing == null) {
                        requestService.create(client.getId(), deviceType, description, priority, cost);
                    } else {
                        requestService.updateDetails(editing.getId(), deviceType, description,
                                priority, status, cost);
                    }
                    return null;
                },
                result -> {
                    saved = true;
                    dialogStage.close();
                },
                error -> {
                    saveButton.setDisable(false);
                    showError(FxTasks.friendly(error));
                });
    }

    /** Пустое поле считается нулём; допускаются запятая и пробелы-разделители тысяч. */
    private BigDecimal parseCost(String raw) {
        String normalized = raw == null ? "" : raw.trim().replace(",", ".").replaceAll("[\\s\u00A0]", "");
        return normalized.isEmpty() ? BigDecimal.ZERO : new BigDecimal(normalized);
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        // окно создано с фиксированным размером: после появления строки ошибки подгоняем его,
        // иначе длинное сообщение обрезается многоточием
        Platform.runLater(dialogStage::sizeToScene);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        Platform.runLater(dialogStage::sizeToScene);
    }
}
