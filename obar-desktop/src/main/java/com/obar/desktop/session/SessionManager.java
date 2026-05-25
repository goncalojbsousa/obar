package com.obar.desktop.session;

import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.enums.UserType;

/**
 * In-memory desktop session holder for the currently authenticated user
 */
public final class SessionManager {

    private static AuthenticatedUserDto currentUser;

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
        currentUser = user;
    }

    /**
     * Returns the currently authenticated user
     *
     * @return authenticated user currently stored in session
     * @throws IllegalStateException when no active session exists
     */
    public static AuthenticatedUserDto getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No active session.");
        }
        return currentUser;
    }

    /**
     * Clears the currently authenticated user
     */
    public static void logout() {
        currentUser = null;
    }

    /**
     * Checks whether there is an active authenticated user
     *
     * @return {@code true} if logged in, otherwise {@code false}
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks whether the current user has the specified role
     *
     * @param role role to verify against the authenticated user
     * @return {@code true} when logged in and the current user has the role,
     *         {@code false} otherwise
     */
    public static boolean hasRole(UserType role) {
        return currentUser != null && currentUser.type() == role;
    }
}
