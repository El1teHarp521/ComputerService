package ru.mirea.computerservice.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Стандартные диалоги JavaFX (Alert). Все они привязываются к окну-владельцу (owner),
 * поэтому всегда появляются поверх нужного окна, а не теряются позади него.
 */
public final class Ui {

    private Ui() {
    }

    public static void info(Window owner, String message) {
        show(Alert.AlertType.INFORMATION, owner, "Информация", message);
    }

    public static void warning(Window owner, String message) {
        show(Alert.AlertType.WARNING, owner, "Внимание", message);
    }

    public static void error(Window owner, String message) {
        show(Alert.AlertType.ERROR, owner, "Ошибка", message);
    }

    public static void error(Window owner, Throwable error) {
        error(owner, FxTasks.friendly(error));
    }

    /** Подтверждение опасного действия (например, удаления). true — пользователь нажал «Да». */
    public static boolean confirm(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(null);
        alert.initOwner(owner);
        return alert.showAndWait().filter(button -> button == ButtonType.YES).isPresent();
    }

    private static void show(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        alert.showAndWait();
    }
}
