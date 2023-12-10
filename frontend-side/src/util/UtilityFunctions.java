package util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class UtilityFunctions {

    public static void showError(String errorStatement) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.getDialogPane().setContent(getText(errorStatement));
            alert.show();
        });
    }

    public static void showConfirmation(String confirmationStatement) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getDialogPane().setContent(getText(confirmationStatement));
            alert.show();
        });
    }
    private static Label getText(String string) {
        Label label = new Label(string);
        label.setPrefWidth(400);
        label.setPadding(new Insets(15, 15, 15, 15));
        return label;
    }
}