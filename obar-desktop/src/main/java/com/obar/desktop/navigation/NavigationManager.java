package com.obar.desktop.navigation;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.PasswordService;
import com.obar.dal.UserRepository;
import com.obar.desktop.auth.ChangePasswordController;
import com.obar.desktop.auth.DashboardController;
import com.obar.desktop.auth.LoginController;
import com.obar.desktop.auth.RegisterController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Centralized desktop-layer navigation utility for switching authentication test views
 */
public final class NavigationManager {

    private static final double WINDOW_WIDTH = 900;
    private static final double WINDOW_HEIGHT = 640;

    private static final PasswordService PASSWORD_SERVICE = new PasswordService();
    private static final AuthService AUTH_SERVICE = new AuthService(new UserRepository(), PASSWORD_SERVICE);

    private static Stage primaryStage;

    private NavigationManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Initializes the navigation utility with the primary JavaFX stage
     *
     * @param stage primary application stage used for all scene transitions
     * @throws IllegalArgumentException when {@code stage} is {@code null}
     */
    public static void initialize(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage must not be null.");
        }
        primaryStage = stage;
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(new StackPane(), WINDOW_WIDTH, WINDOW_HEIGHT));
        }
    }

    /**
     * Navigates to the login screen
     *
     * @throws IllegalStateException when navigation is attempted before initialization
     * @throws RuntimeException when the FXML view cannot be loaded
     */
    public static void navigateToLogin() {
        setScene("/com/obar/desktop/auth/LoginView.fxml");
    }

    /**
     * Navigates to the dashboard screen
     *
     * @throws IllegalStateException when navigation is attempted before initialization
     * @throws RuntimeException when the FXML view cannot be loaded
     */
    public static void navigateToDashboard() {
        setScene("/com/obar/desktop/auth/DashboardView.fxml");
    }

    /**
     * Navigates to the change-password screen
     *
     * @throws IllegalStateException when navigation is attempted before initialization
     * @throws RuntimeException when the FXML view cannot be loaded
     */
    public static void navigateToChangePassword() {
        setScene("/com/obar/desktop/auth/ChangePasswordView.fxml");
    }

    /**
     * Navigates to the registration screen
     *
     * @throws IllegalStateException when navigation is attempted before initialization
     * @throws RuntimeException when the FXML view cannot be loaded
     */
    public static void navigateToRegister() {
        setScene("/com/obar/desktop/auth/RegisterView.fxml");
    }

    private static void setScene(String fxmlPath) {
        if (primaryStage == null) {
            throw new IllegalStateException("NavigationManager must be initialized before navigation.");
        }

        FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
        loader.setControllerFactory(type -> {
            if (type == LoginController.class) {
                return new LoginController(AUTH_SERVICE);
            }
            if (type == DashboardController.class) {
                return new DashboardController(AUTH_SERVICE);
            }
            if (type == ChangePasswordController.class) {
                return new ChangePasswordController(AUTH_SERVICE);
            }
            if (type == RegisterController.class) {
                return new RegisterController(AUTH_SERVICE);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException("Failed to create controller: " + type.getName(), exception);
            }
        });

        try {
            Parent root = loader.load();
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
            } else {
                scene.setRoot(root);
            }
            primaryStage.show();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, exception);
        }
    }
}
