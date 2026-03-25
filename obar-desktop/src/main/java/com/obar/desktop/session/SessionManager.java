package com.obar.desktop.session;

import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.enums.UserType;

/**
 * In-memory desktop session holder for the currently authenticated user
 */
public final class SessionManager {

    private static final ThreadLocal<AuthenticatedUserDto> currentUser = ThreadLocal.withInitial(() -> null);

    private SessionManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Stores the authenticated user in the desktop session
     *
     * @param user authenticated user to keep as current session principal
     * @throws IllegalArgumentException when {@code user} is {@code null}
     */
    public static void login(AuthenticatedUserDto user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        currentUser.set(user);
    }

    /**
     * Returns the currently authenticated user
     *
     * @return authenticated user currently stored in session
     * @throws IllegalStateException when no active session exists
     */
    public static AuthenticatedUserDto getCurrentUser() {
        AuthenticatedUserDto user = currentUser.get();
        if (user == null) {
            throw new IllegalStateException("No active session.");
        }
        return user;
    }

    /**
     * Clears the currently authenticated user
     */
    public static void logout() {
        currentUser.remove();
    }

    /**
     * Checks whether there is an active authenticated user
     *
     * @return {@code true} if logged in, otherwise {@code false}
     */
    public static boolean isLoggedIn() {
        return currentUser.get() != null;
    }

    /**
     * Checks whether the current user has the specified role
     *
     * @param role role to verify against the authenticated user
     * @return {@code true} when logged in and the current user has the role, {@code false} otherwise
     */
    public static boolean hasRole(UserType role) {
        AuthenticatedUserDto user = currentUser.get();
        return user != null && user.type() == role;
    }
}