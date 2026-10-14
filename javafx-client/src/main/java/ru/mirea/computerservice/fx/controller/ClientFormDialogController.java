package ru.mirea.computerservice.fx.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.computerservice.fx.util.FxTasks;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.service.ClientService;

/**
 * Контроллер окна «Клиент». Одна форма работает в двух режимах: создание и редактирование
 * (режим включается вызовом initForEdit() до показа окна).
 *
 * Контроллер НЕ проверяет данные сам: он передаёт значения полей в ClientService и показывает
 * текст ошибки под формой, если сервис отклонил ввод. Правила (обязательность, форматы,
 * длины, уникальность) лежат в сервисе — один раз для любого интерфейса.
 */
public class ClientFormDialogController {

    @FXML private Label titleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private ClientService clientService;
    private Stage dialogStage;
    private Integer editingClientId; // null => режим создания
    private boolean saved;

    /** Вызывается автоматически после внедрения @FXML-полей. */
    @FXML
    private void initialize() {
        saveButton.setDefaultButton(true);   // Enter = «Сохранить»
        cancelButton.setCancelButton(true);  // Esc = «Отмена»
    }

    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Переключает форму в режим редактирования и подставляет текущие значения. */
    public void initForEdit(Client client) {
        this.editingClientId = client.getId();
        titleLabel.setText("Изменить клиента");
        fullNameField.setText(client.getFullName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
    }

    /** MainController смотрит на этот флаг после закрытия окна, чтобы понять, нужно ли обновлять таблицы. */
    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        hideError();
        String fullName = fullNameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        Integer id = editingClientId;

        saveButton.setDisable(true); // защита от двойного нажатия, пока идёт запрос
        FxTasks.run(() -> {
                    if (id == null) {
                        clientService.create(fullName, phone, email);
                    } else {
                        clientService.update(id, fullName, phone, email);
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
