package com.obar;

import atlantafx.base.theme.PrimerLight;
import com.obar.bll.CoreLifecycleService;
import com.obar.desktop.navigation.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Desktop module JavaFX application entry point for manually testing
 * authentication flows
 */
public class App extends Application {

    private final CoreLifecycleService coreLifecycleService = new CoreLifecycleService();

    @Override
    public void init() {
        coreLifecycleService.warmUp();
    }

    /**
     * Starts the JavaFX desktop application
     *
     * @param stage primary JavaFX stage-managed by the runtime
     * @throws RuntimeException when initial view loading fails
     */
    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setTitle("OBAR Desktop");

        NavigationManager.initialize(stage);
        NavigationManager.navigateToLogin();
    }

    @Override
    public void stop() {
        coreLifecycleService.shutdown();
    }

}
