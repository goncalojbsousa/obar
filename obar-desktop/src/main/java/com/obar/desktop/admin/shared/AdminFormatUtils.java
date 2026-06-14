package com.obar.desktop.admin.shared;

import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.PaymentStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.model.enums.UserType;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Shared formatting and normalization helpers for admin UI controllers.
 */
public final class AdminFormatUtils {

    private AdminFormatUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String prettyStatus(AccountStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case ACTIVE -> "Online";
            case INACTIVE -> "Offline";
            case BLOCKED -> "Bloqueado";
            case PENDING -> "Pendente";
        };
    }

    public static String prettyUserType(UserType type) {
        if (type == null) {
            return "-";
        }

        return switch (type) {
            case ADMIN -> "Administrador";
            case DRIVER -> "Motorista";
            case CLIENT -> "Cliente";
        };
    }

    public static String prettyTripStatus(TripStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case PENDING -> "Pendente";
            case ACCEPTED -> "Aceite";
            case IN_PROGRESS -> "Em progresso";
            case COMPLETED -> "Concluida";
            case CANCELLED -> "Cancelada";
            case REJECTED -> "Rejeitada";
        };
    }

    public static String prettyTripType(TripType tripType) {
        if (tripType == null) {
            return "-";
        }

        return switch (tripType) {
            case IMMEDIATE -> "Imediata";
            case SCHEDULED -> "Agendada";
        };
    }

    public static String prettyPaymentStatus(PaymentStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case PENDING -> "Pendente";
            case PROCESSED -> "Processado";
            case FAILED -> "Falhado";
            case REFUNDED -> "Reembolsado";
        };
    }

    public static String prettyPaymentMethod(String rawType) {
        String normalized = normalize(rawType);
        if (normalized.isBlank() || normalized.equals("-")) {
            return "Outros";
        }
        if (normalized.contains("mb")) {
            return "MB Way";
        }
        if (normalized.contains("paypal")) {
            return "PayPal";
        }
        if (normalized.contains("visa")) {
            return "Visa";
        }
        if (normalized.contains("master")) {
            return "Mastercard";
        }
        if (normalized.contains("cartao") || normalized.contains("card") || normalized.contains("credito")) {
            return "Cartao";
        }
        return rawType.trim();
    }

    public static String prettyFinancialPeriod(AdminFinancialPeriod period) {
        if (period == null) {
            return "-";
        }
        return switch (period) {
            case DAY -> "Ultimo dia";
            case WEEK -> "Ultima semana";
            case MONTH -> "Ultimo mes";
            case YEAR -> "Ultimo ano";
            case ALL -> "Todo o historico";
        };
    }

    public static String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "0.00 \u20AC";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP) + " \u20AC";
    }

    public static String formatTripPrice(AdminTripDTO trip) {
        BigDecimal amount = trip.finalPrice() != null ? trip.finalPrice() : trip.estimatedPrice();
        if (amount == null) {
            return "-";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP) + " \u20AC";
    }

    public static String formatPaymentAmount(AdminPaymentByTripDTO payment) {
        if (payment.amount() == null) {
            return "-";
        }
        return payment.amount().setScale(2, java.math.RoundingMode.HALF_UP) + " \u20AC";
    }

    public static String formatTaxRateDisplay(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            return "-";
        }
        return fallback(taxRate.name()) + " (" + taxRate.rate() + ")";
    }

    public static String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    public static String starRating(Float rating) {
        if (rating == null) {
            return "-";
        }
        return String.format(Locale.US, "%.1f", rating);
    }

    public static int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    public static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static String fallback(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String extractInitials(String name) {
        String safeName = fallback(name);
        if (safeName.equals("-")) {
            return "--";
        }

        String[] parts = safeName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }

        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    public static Integer parseRequiredInteger(String value, String fieldName) {
        Integer parsed = parseOptionalInteger(value, fieldName);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parsed;
    }

    public static Integer parseOptionalInteger(String value, String fieldName) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " deve ser um numero inteiro.");
        }
    }

    public static BigDecimal parseRequiredDecimal(String value, String fieldName) {
        BigDecimal parsed = parseOptionalDecimal(value, fieldName);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parsed;
    }

    public static BigDecimal parseOptionalDecimal(String value, String fieldName) {
        String text = value == null ? "" : value.trim().replace(",", ".");
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " deve ser um valor numerico.");
        }
    }
}
