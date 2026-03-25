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

    private static final int MIN_PASSWORD_LENGTH = 8;

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

        String passwordValidationMessage = validatePasswordComplexity(password);
        if (passwordValidationMessage != null) {
            showMessage(passwordValidationMessage);
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

    private String validatePasswordComplexity(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            return "Password must contain at least one number.";
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            return "Password must contain at least one uppercase letter.";
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            return "Password must contain at least one lowercase letter.";
        }
        boolean hasSpecialCharacter = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasSpecialCharacter) {
            return "Password must contain at least one special character.";
        }
        return null;
    }
}
