package com.obar.desktop.auth;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.bll.auth.AuthenticationException;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import com.obar.model.enums.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

/**
 * JavaFX controller for the desktop registration screen.
 */
public class RegisterController {

    private final AuthService authService;

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private TextField     referenceField;
    @FXML private Label         referenceLabel;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<UserType> accountTypeComboBox;
    @FXML private Label         messageLabel;
    @FXML private StackPane     driverPendingOverlay;
    @FXML private Label         driverPendingMessageLabel;

    public RegisterController(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("AuthService must not be null.");
        }
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        accountTypeComboBox.getItems().setAll(UserType.CLIENT, UserType.DRIVER);
        accountTypeComboBox.setValue(UserType.CLIENT);
        accountTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserType type) {
                if (type == null) return "";
                return type == UserType.DRIVER ? "Motorista" : "Cliente";
            }

            @Override
            public UserType fromString(String value) {
                if (value == null) return UserType.CLIENT;
                return "Motorista".equalsIgnoreCase(value.trim()) ? UserType.DRIVER : UserType.CLIENT;
            }
        });

        updateReferenceLabel(UserType.CLIENT);

        if (driverPendingOverlay != null) {
            driverPendingOverlay.setVisible(false);
            driverPendingOverlay.setManaged(false);
        }
    }

    @FXML
    public void handleAccountTypeChanged() {
        updateReferenceLabel(accountTypeComboBox.getValue());
    }

    @FXML
    public void handleRegister() {
        hideMessage();

        String name      = safe(nameField.getText());
        String email     = safe(emailField.getText());
        String phone     = safe(phoneField.getText());
        String reference = safe(referenceField.getText());
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();

        if (name.isBlank() || email.isBlank() || password == null || password.isBlank()) {
            showMessage("Nome, email e palavra-passe são obrigatórios.");
            return;
        }

        if (!password.equals(confirm)) {
            showMessage("As palavras-passe não coincidem.");
            return;
        }

        UserType selectedType = accountTypeComboBox.getValue() == null
                ? UserType.CLIENT
                : accountTypeComboBox.getValue();

        try {
            AuthenticatedUserDto user = authService.register(
                    name, email, password, selectedType,
                    phone.isBlank() ? null : phone,
                    reference.isBlank() ? null : reference);

            if (selectedType == UserType.DRIVER) {
                showDriverPendingOverlay(name);
                return;
            }

            SessionManager.login(user);
            NavigationManager.navigateToLogin();

        } catch (IllegalArgumentException | AuthenticationException e) {
            showMessage(e.getMessage());
        }
    }

    @FXML
    public void handlePendingGoToLogin() {
        NavigationManager.navigateToLogin();
    }

    @FXML
    public void handleBackToLogin() {
        NavigationManager.navigateToLogin();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void updateReferenceLabel(UserType type) {
        if (referenceLabel == null || referenceField == null) return;
        boolean isDriver = type == UserType.DRIVER;
        referenceLabel.setText(isDriver ? "Nº DE LICENÇA" : "NIF");
        referenceField.setPromptText(isDriver ? "Ex: PT123456789" : "000000000");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private void showDriverPendingOverlay(String name) {
        if (driverPendingMessageLabel != null) {
            String prefix = name.isBlank() ? "A tua conta" : "A conta de " + name;
            driverPendingMessageLabel.setText(
                    prefix + " foi criada como motorista e está pendente de validação por um admin.");
        }
        if (driverPendingOverlay != null) {
            driverPendingOverlay.setVisible(true);
            driverPendingOverlay.setManaged(true);
        }
    }
}
