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
 * JavaFX controller for the desktop authentication login screen
 */
public class LoginController {

    private final AuthService authService;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    /**
     * Creates a login controller with the authentication service dependency
     *
     * @param authService authentication service used for login attempts
     * @throws IllegalArgumentException when {@code authService} is {@code null}
     */
    public LoginController(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("AuthService must not be null.");
        }
        this.authService = authService;
    }

    /**
     * Handles login button actions by authenticating credentials and navigating on
     * success
     *
     * @throws RuntimeException when authentication dependencies fail unexpectedly
     */
    @FXML
    public void handleLogin() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        try {
            AuthenticatedUserDto authenticatedUser = authService.authenticate(
                    emailField.getText(),
                    passwordField.getText());

            SessionManager.login(authenticatedUser);
            if (authenticatedUser.type() == UserType.ADMIN) {
                NavigationManager.navigateToAdmin();
            } else {
                NavigationManager.navigateToDashboard();
            }
        } catch (AuthenticationException exception) {
            errorLabel.setText(exception.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Opens the registration screen.
     *
     * @throws RuntimeException when view loading fails
     */
    @FXML
    public void handleGoToRegister() {
        NavigationManager.navigateToRegister();
    }
}
