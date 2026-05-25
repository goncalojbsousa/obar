package com.obar.bll.admin;

import java.util.Locale;

/**
 * Small text helper for admin BLL commands.
 */
final class AdminTextSanitizer {

    private AdminTextSanitizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static String trimOrNull(String value) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? null : safeValue;
    }

    static String normalizeEmail(String email) {
        return safe(email).toLowerCase(Locale.ROOT);
    }
}
