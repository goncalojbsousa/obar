package com.obar.desktop.navigation;

import com.obar.bll.admin.AdminService;
import com.obar.bll.auth.AuthService;
import com.obar.desktop.admin.AdminController;
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
 * Centralized desktop-layer navigation utility for switching authentication
 * test views.
 */
public final class NavigationManager {

    private static final double WINDOW_WIDTH = 900;
    private static final double WINDOW_HEIGHT = 640;
    private static final String DESKTOP_STYLESHEET = "/com/obar/desktop/styles/DesktopTheme.css";

    private static final AuthService AUTH_SERVICE = new AuthService();
    private static final AdminService ADMIN_SERVICE = new AdminService();

    private static Stage primaryStage;

    private NavigationManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Initializes the navigation utility with the primary JavaFX stage.
     *
     * @param stage primary application stage used for all scene transitions
     */
    public static void initialize(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage must not be null.");
        }

        primaryStage = stage;
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(new StackPane(), WINDOW_WIDTH, WINDOW_HEIGHT));
        }
        addDesktopStylesheet();
    }

    public static void navigateToLogin() {
        setScene("/com/obar/desktop/auth/LoginView.fxml");
    }

    public static void navigateToDashboard() {
        setScene("/com/obar/desktop/auth/DashboardView.fxml");
    }

    public static void navigateToChangePassword() {
        setScene("/com/obar/desktop/auth/ChangePasswordView.fxml");
    }

    public static void navigateToRegister() {
        setScene("/com/obar/desktop/auth/RegisterView.fxml");
    }

    public static void navigateToAdmin() {
        setScene("/com/obar/desktop/admin/AdminView.fxml");
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
            if (type == AdminController.class) {
                return new AdminController(ADMIN_SERVICE);
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
                addDesktopStylesheet();
            } else {
                scene.setRoot(root);
            }
            primaryStage.show();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, exception);
        }
    }

    private static void addDesktopStylesheet() {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            return;
        }

        java.net.URL stylesheetUrl = NavigationManager.class.getResource(DESKTOP_STYLESHEET);
        if (stylesheetUrl == null) {
            return;
        }

        String stylesheet = stylesheetUrl.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
