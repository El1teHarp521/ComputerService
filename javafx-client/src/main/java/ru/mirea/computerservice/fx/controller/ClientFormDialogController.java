package ru.mirea.computerservice.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.computerservice.exception.BusinessException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.service.ClientService;

/**
 * Контроллер диалогового окна "Клиент". Одна и та же форма используется
 * и для создания, и для редактирования — режим определяется тем, вызван
 * ли initForEdit() перед показом окна.
 *
 * Контроллер НЕ содержит бизнес-правил — он только читает значения полей
 * и передаёт их в ClientService, а полученное исключение превращает
 * в текст под формой. Вся логика (обязательность ФИО, уникальность телефона
 * и т.д.) находится в сервисе — ровно так же, как в консольной версии КР1.
 */
public class ClientFormDialogController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private Label errorLabel;

    private ClientService clientService;
    private Stage dialogStage;
    private Client editingClient; // null => форма работает в режиме создания
    private boolean saved = false;

    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Переключает форму в режим редактирования и подставляет текущие значения клиента. */
    public void initForEdit(Client client) {
        this.editingClient = client;
        titleLabel.setText("Изменить клиента");
        fullNameField.setText(client.getFullName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
    }

    /** MainController проверяет этот флаг после закрытия окна, чтобы понять, нужно ли обновлять таблицу. */
    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        try {
            String fullName = fullNameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();

            if (editingClient == null) {
                clientService.create(fullName, phone, email.isEmpty() ? null : email);
            } else {
                editingClient.setFullName(fullName);
                editingClient.setPhone(phone);
                editingClient.setEmail(email.isEmpty() ? null : email);
                clientService.update(editingClient);
            }
            saved = true;
            dialogStage.close();
        } catch (BusinessException e) {
            // Нарушение бизнес-правила (пустое ФИО, занятый телефон) — показываем прямо в форме
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Непредвиденная ошибка: " + e.getMessage());
        }
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
