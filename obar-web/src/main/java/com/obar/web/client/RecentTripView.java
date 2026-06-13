package com.obar.web.client;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record RecentTripView(
        Integer id,
        String routeLabel,
        String originAddress,
        String destinationAddress,
        LocalDateTime referenceTime,
        BigDecimal price,
        String vehicleCategory,
        String driverName,
        TripStatus status,
        Float distanceKm) {

    private static final Locale PT_LOCALE = Locale.forLanguageTag("pt-PT");

    public static RecentTripView from(Trip trip) {
        return new RecentTripView(
                trip.getId(),
                routeLabel(trip),
                trip.getRoute() == null ? null : trip.getRoute().getOriginAddress(),
                trip.getRoute() == null ? null : trip.getRoute().getDestinationAddress(),
                referenceTime(trip),
                price(trip),
                valueOrFallback(trip.getVehicleCategory(), "-"),
                trip.getDriver() == null ? null : trip.getDriver().getName(),
                trip.getStatus(),
                trip.getRoute() == null ? null : trip.getRoute().getDistanceKm());
    }

    public String originDisplay() {
        return valueOrFallback(originAddress, "Origem");
    }

    public String destinationDisplay() {
        return valueOrFallback(destinationAddress, "Destino");
    }

    public String dateLabel() {
        if (referenceTime == null) {
            return "-";
        }
        return DateTimeFormatter.ofPattern("d MMM yyyy", PT_LOCALE).format(referenceTime);
    }

    public String timeLabel() {
        if (referenceTime == null) {
            return "-";
        }
        return referenceTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String distanceLabel() {
        if (distanceKm == null) {
            return "-";
        }
        return String.format(PT_LOCALE, "%.1f km", distanceKm);
    }

    public String statusLabel() {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case COMPLETED -> "Conclu\u00EDda";
            case CANCELLED -> "Cancelada";
            case REJECTED -> "Rejeitada";
            case PENDING -> "A confirmar";
            case ACCEPTED -> "Agendada";
            case IN_PROGRESS -> "Em curso";
        };
    }

    public String statusClass() {
        if (status == TripStatus.COMPLETED) {
            return "is-completed";
        }
        if (status == TripStatus.CANCELLED || status == TripStatus.REJECTED) {
            return "is-cancelled";
        }
        if (status == TripStatus.ACCEPTED) {
            return "is-confirmed";
        }
        return "is-pending";
    }

    private static LocalDateTime referenceTime(Trip trip) {
        if (trip.getEndTime() != null) {
            return trip.getEndTime();
        }
        if (trip.getScheduledTime() != null) {
            return trip.getScheduledTime();
        }
        return trip.getRequestTime();
    }

    private static BigDecimal price(Trip trip) {
        if (trip.getFinalPrice() != null) {
            return trip.getFinalPrice();
        }
        if (trip.getEstimatedPrice() != null) {
            return trip.getEstimatedPrice();
        }
        return BigDecimal.ZERO;
    }

    private static String routeLabel(Trip trip) {
        if (trip.getRoute() == null) {
            return "Rota indisponivel";
        }
        return valueOrFallback(trip.getRoute().getOriginAddress(), "Origem")
                + " -> "
                + valueOrFallback(trip.getRoute().getDestinationAddress(), "Destino");
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
