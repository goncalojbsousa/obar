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
 * JavaFX controller for the desktop registration screen
 */
public class RegisterController {

    private final AuthService authService;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<UserType> accountTypeComboBox;

    @FXML
    private Label messageLabel;

    @FXML
    private StackPane driverPendingOverlay;

    @FXML
    private Label driverPendingMessageLabel;

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
            public String toString(UserType userType) {
                if (userType == null) {
                    return "";
                }
                return userType == UserType.DRIVER ? "Motorista" : "Cliente";
            }

            @Override
            public UserType fromString(String value) {
                if (value == null) {
                    return UserType.CLIENT;
                }
                return "Motorista".equalsIgnoreCase(value.trim()) ? UserType.DRIVER : UserType.CLIENT;
            }
        });

        if (driverPendingOverlay != null) {
            driverPendingOverlay.setVisible(false);
            driverPendingOverlay.setManaged(false);
        }
    }

    @FXML
    public void handleRegister() {
        hideMessage();

        String name = safe(nameField.getText());
        String email = safe(emailField.getText());
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (name.isBlank() || email.isBlank() || password == null || password.isBlank()) {
            showMessage("Name, email and password are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Password and confirmation do not match.");
            return;
        }

        UserType selectedType = accountTypeComboBox.getValue() == null
                ? UserType.CLIENT
                : accountTypeComboBox.getValue();

        try {
            AuthenticatedUserDto authenticatedUser = authService.register(name, email, password, selectedType);
            if (selectedType == UserType.DRIVER) {
                showDriverPendingOverlay(name);
                return;
            }
            SessionManager.login(authenticatedUser);
            NavigationManager.navigateToDashboard();
        } catch (IllegalArgumentException | AuthenticationException exception) {
            showMessage(exception.getMessage());
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
            String displayName = safe(name);
            String prefix = displayName.isBlank() ? "A tua conta" : "A conta de " + displayName;
            driverPendingMessageLabel.setText(prefix
                    + " foi criada como motorista e está pendente de validação por um admin.");
        }

        if (driverPendingOverlay != null) {
            driverPendingOverlay.setVisible(true);
            driverPendingOverlay.setManaged(true);
        }
    }
}
