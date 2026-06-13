package com.obar.desktop.navigation;

import com.obar.desktop.app.DesktopApplicationContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Centralized desktop-layer navigation utility for switching JavaFX screens.
 */
public final class NavigationManager {

    private static final double WINDOW_WIDTH = 900;
    private static final double WINDOW_HEIGHT = 640;
    private static final List<String> DESKTOP_STYLESHEETS = List.of(
            "/com/obar/desktop/styles/DesktopTheme.css",
            "/com/obar/desktop/styles/admin/sidebar.css",
            "/com/obar/desktop/styles/admin/tables.css",
            "/com/obar/desktop/styles/admin/details.css",
            "/com/obar/desktop/styles/admin/financial.css",
            "/com/obar/desktop/styles/admin/approvals.css",
            "/com/obar/desktop/styles/admin/modals.css");

    private static Stage primaryStage;
    private static DesktopApplicationContext applicationContext;

    private NavigationManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Initializes the navigation utility with the primary JavaFX stage.
     *
     * @param stage primary application stage used for all scene transitions
     */
    public static void initialize(Stage stage, DesktopApplicationContext context) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage must not be null.");
        }
        if (context == null) {
            throw new IllegalArgumentException("DesktopApplicationContext must not be null.");
        }

        primaryStage = stage;
        applicationContext = context;
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(new StackPane(), WINDOW_WIDTH, WINDOW_HEIGHT));
        }
        addDesktopStylesheet();
    }

    public static void navigateToLogin() {
        setScene("/com/obar/desktop/auth/LoginView.fxml");
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
        if (applicationContext == null) {
            throw new IllegalStateException("DesktopApplicationContext must be initialized before navigation.");
        }

        FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
        loader.setControllerFactory(applicationContext::createController);

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

        for (String stylesheetPath : DESKTOP_STYLESHEETS) {
            java.net.URL stylesheetUrl = NavigationManager.class.getResource(stylesheetPath);
            if (stylesheetUrl == null) {
                continue;
            }

            String stylesheet = stylesheetUrl.toExternalForm();
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
        }
    }
}
