package com.obar.web.client;

import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.enums.TripStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record ClientProfileView(
        String name,
        String initials,
        String email,
        String phone,
        String taxNumber,
        String accountStatus,
        String memberSince,
        int totalTrips,
        int completedTrips,
        int scheduledTrips,
        String lastTripDate,
        String lastTripRoute,
        String lastTripStatus) {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy",
            Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy",
            Locale.forLanguageTag("pt-PT"));

    public static ClientProfileView from(User user, List<Trip> trips) {
        List<Trip> safeTrips = trips == null ? List.of() : trips;
        Trip lastTrip = safeTrips.stream()
                .max(Comparator.comparing(ClientProfileView::tripReferenceTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        return new ClientProfileView(
                valueOrFallback(user.getName(), "-"),
                initials(user.getName()),
                valueOrFallback(user.getEmail(), "-"),
                valueOrFallback(user.getPhone(), "-"),
                valueOrFallback(user.getTaxNumber(), "-"),
                statusLabel(user),
                formatMonth(user.getCreatedAt()),
                safeTrips.size(),
                (int) safeTrips.stream().filter(trip -> trip.getStatus() == TripStatus.COMPLETED).count(),
                (int) safeTrips.stream().filter(ClientProfileView::isScheduledTrip).count(),
                lastTrip == null ? "-" : formatDate(tripReferenceTime(lastTrip)),
                lastTrip == null ? "Ainda sem viagens" : routeLabel(lastTrip),
                lastTrip == null ? "-" : tripStatusLabel(lastTrip.getStatus()));
    }

    private static boolean isScheduledTrip(Trip trip) {
        return trip != null
                && trip.getScheduledTime() != null
                && trip.getStatus() != TripStatus.CANCELLED
                && trip.getStatus() != TripStatus.COMPLETED;
    }

    private static LocalDateTime tripReferenceTime(Trip trip) {
        if (trip == null) {
            return null;
        }
        if (trip.getScheduledTime() != null) {
            return trip.getScheduledTime();
        }
        return trip.getRequestTime();
    }

    private static String routeLabel(Trip trip) {
        if (trip.getRoute() == null) {
            return "Rota indisponivel";
        }
        return valueOrFallback(trip.getRoute().getOriginAddress(), "Origem")
                + " -> "
                + valueOrFallback(trip.getRoute().getDestinationAddress(), "Destino");
    }

    private static String statusLabel(User user) {
        if (user.getStatus() == null) {
            return "-";
        }
        return switch (user.getStatus()) {
            case ACTIVE -> "Ativa";
            case INACTIVE -> "Inativa";
            case BLOCKED -> "Bloqueada";
            case PENDING -> "Pendente";
        };
    }

    private static String tripStatusLabel(TripStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "Pendente";
            case ACCEPTED -> "Aceite";
            case IN_PROGRESS -> "Em curso";
            case COMPLETED -> "Concluida";
            case CANCELLED -> "Cancelada";
            case REJECTED -> "Rejeitada";
        };
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "C";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private static String formatMonth(LocalDateTime value) {
        return value == null ? "-" : MONTH_FORMATTER.format(value);
    }

    private static String formatDate(LocalDateTime value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
