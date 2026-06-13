package com.obar.web.session;

import com.obar.bll.auth.AuthenticatedUserDto;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public final class WebSessionHelper {

    private static final String SESSION_KEY = "authenticatedUser";

    private WebSessionHelper() {
    }

    public static void login(HttpSession session, AuthenticatedUserDto user) {
        session.setAttribute(SESSION_KEY, user);
    }

    public static Optional<AuthenticatedUserDto> getCurrentUser(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof AuthenticatedUserDto authenticatedUser
                ? Optional.of(authenticatedUser)
                : Optional.empty();
    }

    public static boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session).isPresent();
    }

    public static void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }
}
