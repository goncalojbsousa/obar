package com.obar.desktop.app;

import com.obar.bll.admin.AdminService;
import com.obar.bll.auth.AuthService;
import com.obar.desktop.admin.AdminController;
import com.obar.desktop.auth.ChangePasswordController;
import com.obar.desktop.auth.LoginController;
import com.obar.desktop.auth.RegisterController;

/**
 * Creates the small set of objects shared by the desktop application.
 *
 * <p>
 * This is the only desktop class that creates services from {@code obar-core}.
 * The desktop module depends on the Maven artifact {@code obar-core.jar} and
 * uses the public BLL services exposed by that module.
 * </p>
 */
public final class DesktopApplicationContext {

    private final AuthService authenticationService;
    private final AdminService adminService;

    public DesktopApplicationContext() {
        this.authenticationService = new AuthService();
        this.adminService = new AdminService();
    }

    public AuthService getAuthenticationService() {
        return authenticationService;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    /**
     * Creates JavaFX controllers requested by {@link javafx.fxml.FXMLLoader}.
     * Controllers that need core services receive them through their
     * constructors.
     */
    public Object createController(Class<?> controllerType) {
        if (controllerType == LoginController.class) {
            return new LoginController(authenticationService);
        }
        if (controllerType == ChangePasswordController.class) {
            return new ChangePasswordController(authenticationService);
        }
        if (controllerType == RegisterController.class) {
            return new RegisterController(authenticationService);
        }
        if (controllerType == AdminController.class) {
            return new AdminController(adminService);
        }
        return createControllerWithDefaultConstructor(controllerType);
    }

    private Object createControllerWithDefaultConstructor(Class<?> controllerType) {
        try {
            return controllerType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to create controller: " + controllerType.getName(), exception);
        }
    }
}
