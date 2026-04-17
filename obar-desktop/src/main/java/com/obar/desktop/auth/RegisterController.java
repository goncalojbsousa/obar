package com.obar.desktop.auth;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.bll.auth.AuthenticationException;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import com.obar.model.enums.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private Label messageLabel;

    public RegisterController(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("AuthService must not be null.");
        }
        this.authService = authService;
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

        try {
            AuthenticatedUserDto authenticatedUser = authService.register(name, email, password, UserType.CLIENT);
            SessionManager.login(authenticatedUser);
            NavigationManager.navigateToDashboard();
        } catch (IllegalArgumentException | AuthenticationException exception) {
            showMessage(exception.getMessage());
        }
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
}
