package com.obar.desktop.auth;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticationException;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/**
 * JavaFX controller for the desktop password update screen
 */
public class ChangePasswordController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthService authService;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmNewPasswordField;

    @FXML
    private Label messageLabel;

    /**
     * Creates a change-password controller with the authentication service dependency
     *
     * @param authService authentication service used for password updates
     * @throws IllegalArgumentException when {@code authService} is {@code null}
     */
    public ChangePasswordController(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("AuthService must not be null.");
        }
        this.authService = authService;
    }

    /**
     * Saves the new password when local confirmation succeeds
     *
     * @throws IllegalStateException when no authenticated session exists
     * @throws RuntimeException when navigation fails after a successful update
     */
    @FXML
    public void handleSave() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmNewPasswordField.getText();

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            messageLabel.setText("New password and confirmation do not match.");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            return;
        }

        String passwordValidationMessage = validatePasswordComplexity(newPassword);
        if (passwordValidationMessage != null) {
            messageLabel.setText(passwordValidationMessage);
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
            return;
        }

        try {
            authService.changePassword(
                    SessionManager.getCurrentUser().id(),
                    currentPasswordField.getText(),
                    newPassword);
            NavigationManager.navigateToDashboard();
        } catch (AuthenticationException exception) {
            messageLabel.setText(exception.getMessage());
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
    }

    /**
     * Cancels password update and returns to the dashboard
     *
     * @throws RuntimeException when view loading fails
     */
    @FXML
    public void handleCancel() {
        NavigationManager.navigateToDashboard();
    }

    private String validatePasswordComplexity(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "New password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            return "New password must contain at least one number.";
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            return "New password must contain at least one uppercase letter.";
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            return "New password must contain at least one lowercase letter.";
        }
        boolean hasSpecialCharacter = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasSpecialCharacter) {
            return "New password must contain at least one special character.";
        }
        return null;
    }
}
