package com.obar.desktop.auth;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * JavaFX controller for the desktop authentication dashboard screen
 */
public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label statusLabel;

    /**
     * Creates a dashboard controller with the authentication service dependency
     *
     * @param authService authentication service shared across desktop auth flows
     * @throws IllegalArgumentException when {@code authService} is {@code null}
     */
    public DashboardController(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("AuthService must not be null.");
        }
    }

    /**
     * Initializes dashboard labels with currently authenticated user details
     *
     * @throws IllegalStateException when no authenticated session exists
     */
    @FXML
    public void initialize() {
        AuthenticatedUserDto currentUser = SessionManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + currentUser.name());
        roleLabel.setText("Role: " + currentUser.type());
        statusLabel.setText("Status: " + currentUser.status());
    }

    /**
     * Navigates to the change-password screen
     *
     * @throws RuntimeException when view loading fails
     */
    @FXML
    public void handleChangePassword() {
        NavigationManager.navigateToChangePassword();
    }

    /**
     * Ends the session and returns to the login screen
     *
     * @throws RuntimeException when view loading fails
     */
    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationManager.navigateToLogin();
    }
}
