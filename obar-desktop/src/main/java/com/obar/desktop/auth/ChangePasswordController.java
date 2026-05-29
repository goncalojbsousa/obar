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
     * Creates a change-password controller with the authentication service
     * dependency
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
     * @throws RuntimeException      when navigation fails after a successful update
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

        try {
            authService.changePassword(
                    SessionManager.getCurrentUser().id(),
                    currentPasswordField.getText(),
                    newPassword);
            NavigationManager.navigateToAdmin();
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
        NavigationManager.navigateToAdmin();
    }
}
