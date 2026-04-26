package com.obar.desktop.admin.shared;

import java.math.BigDecimal;

/**
 * Parsing helpers shared across admin section controllers.
 */
public final class AdminParseUtils {

    private AdminParseUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Integer parseRequiredInteger(String rawValue, String fieldName) {
        Integer parsed = parseOptionalInteger(rawValue);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parsed;
    }

    public static Integer parseOptionalInteger(String rawValue) {
        String safe = rawValue == null ? "" : rawValue.trim();
        if (safe.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(safe);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID invalido: " + safe);
        }
    }

    public static BigDecimal parseRequiredDecimal(String rawValue, String fieldName) {
        BigDecimal parsed = parseOptionalDecimal(rawValue);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parsed;
    }

    public static BigDecimal parseOptionalDecimal(String rawValue) {
        String safe = rawValue == null ? "" : rawValue.trim();
        if (safe.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(safe);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor monetario invalido: " + safe);
        }
    }
}
