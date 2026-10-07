package ru.mirea.computerservice.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.service.RepairRequestService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Контроллер диалогового окна "Заявка на ремонт".
 * Клиент и статус выбираются из ComboBox, а не вводятся вручную —
 * это требование ТЗ про работу со связанными сущностями через интерфейс,
 * а не через ручной ввод ID.
 */
public class RequestFormDialogController {

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Client> clientCombo;
    @FXML
    private TextField deviceTypeField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<Priority> priorityCombo;
    @FXML
    private ComboBox<RequestStatus> statusCombo;
    @FXML
    private TextField costField;
    @FXML
    private Label errorLabel;

    private RepairRequestService requestService;
    private Stage dialogStage;
    private RepairRequest editingRequest; // null => форма работает в режиме создания
    private boolean saved = false;

    public void setRequestService(RepairRequestService requestService) {
        this.requestService = requestService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Заполняет выпадающие списки. Вызывается MainController ещё до показа окна,
     * сразу после загрузки FXML — чтобы к моменту открытия формы списки уже были готовы.
     */
    public void initLookups(List<Client> clients) {
        clientCombo.setItems(FXCollections.observableArrayList(clients));
        priorityCombo.setItems(FXCollections.observableArrayList(Priority.values()));
        statusCombo.setItems(FXCollections.observableArrayList(RequestStatus.values()));

        priorityCombo.getSelectionModel().select(Priority.NORMAL);
        statusCombo.getSelectionModel().select(RequestStatus.NEW);
        // При создании новая заявка всегда получает статус NEW — его смена
        // станет доступна только после сохранения, через режим редактирования
        statusCombo.setDisable(true);
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
        // Заявку нельзя переназначить другому клиенту задним числом — поле заблокировано
        clientCombo.setDisable(true);

        deviceTypeField.setText(request.getDeviceType());
        descriptionArea.setText(request.getProblemDescription());
        priorityCombo.getSelectionModel().select(request.getPriority());
        statusCombo.getSelectionModel().select(request.getStatus());
        statusCombo.setDisable(false); // при редактировании статус менять можно
        costField.setText(request.getCost() == null ? "0" : request.getCost().toPlainString());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        try {
            Client client = clientCombo.getValue();
            if (client == null) {
                showError("Выберите клиента из списка");
                return;
            }
            String deviceType = deviceTypeField.getText().trim();
            String description = descriptionArea.getText().trim();
            Priority priority = priorityCombo.getValue();
            BigDecimal cost = parseCost(costField.getText());

            if (editingRequest == null) {
                requestService.create(client.getId(), deviceType, description, priority);
            } else {
                RequestStatus status = statusCombo.getValue();
                requestService.updateDetails(editingRequest.getId(), deviceType, description,
                        priority, status, cost);
            }
            saved = true;
            dialogStage.close();
        } catch (BusinessException e) {
            // Например: "Нельзя изменить статус завершённой или отменённой заявки"
            showError(e.getMessage());
        } catch (NumberFormatException e) {
            showError("Стоимость должна быть числом, например 1500.00");
        } catch (Exception e) {
            showError("Непредвиденная ошибка: " + e.getMessage());
        }
    }

    private BigDecimal parseCost(String raw) {
        String normalized = (raw == null || raw.isBlank()) ? "0" : raw.trim().replace(",", ".");
        return new BigDecimal(normalized);
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
