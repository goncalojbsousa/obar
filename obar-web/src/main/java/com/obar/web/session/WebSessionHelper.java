package com.obar.web.session;

import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.enums.UserType;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/**
 * HTTP-session helper for storing and retrieving authenticated users in web requests
 */
public final class WebSessionHelper {

    private static final String SESSION_KEY = "authenticatedUser";

    private WebSessionHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Stores an authenticated user in the current HTTP session.
     *
     * @param session active HTTP session
     * @param user authenticated user to store
     * @throws IllegalArgumentException when {@code session} or {@code user} is {@code null}
     */
    public static void login(HttpSession session, AuthenticatedUserDto user) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        session.setAttribute(SESSION_KEY, user);
    }

    /**
     * Returns the authenticated user currently stored in session.
     *
     * @param session active HTTP session
     * @return optional containing the authenticated user DTO when present; empty optional when absent
     * @throws IllegalArgumentException when {@code session} is {@code null}
     */
    public static Optional<AuthenticatedUserDto> getCurrentUser(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof AuthenticatedUserDto authenticatedUser
                ? Optional.of(authenticatedUser)
                : Optional.empty();
    }

    /**
     * Checks whether there is an authenticated user in session.
     *
     * @param session active HTTP session
     * @return {@code true} when a user is present in session; otherwise {@code false}
     * @throws IllegalArgumentException when {@code session} is {@code null}
     */
    public static boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session).isPresent();
    }

    /**
     * Logs out by invalidating the whole HTTP session.
     *
     * @param session active HTTP session
     * @throws IllegalArgumentException when {@code session} is {@code null}
     */
    public static void logout(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }
        // Invalidate full session, not just the attribute.
        session.invalidate();
    }

    /**
     * Checks whether the authenticated user has the given role.
     *
     * @param session active HTTP session
     * @param role role to validate
     * @return {@code true} when logged in and role matches; {@code false} otherwise
     * @throws IllegalArgumentException when {@code session} is {@code null}
     */
    public static boolean hasRole(HttpSession session, UserType role) {
        return getCurrentUser(session)
                .map(user -> user.type() == role)
                .orElse(false);
    }
}