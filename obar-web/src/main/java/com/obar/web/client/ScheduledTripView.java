package com.obar.web.client;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record ScheduledTripView(
        Integer id,
        String originAddress,
        String destinationAddress,
        String vehicleCategory,
        TripStatus status,
        LocalDateTime scheduledTime,
        BigDecimal estimatedPrice,
        String driverName,
        String vehicleDisplay,
        Float distanceKm,
        Integer durationMin) {

    private static final Locale PT_LOCALE = Locale.forLanguageTag("pt-PT");
    private static final DateTimeFormatter DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("d MMM", PT_LOCALE);

    public static ScheduledTripView from(Trip trip) {
        return new ScheduledTripView(
                trip.getId(),
                trip.getRoute() == null ? null : trip.getRoute().getOriginAddress(),
                trip.getRoute() == null ? null : trip.getRoute().getDestinationAddress(),
                trip.getVehicleCategory(),
                trip.getStatus(),
                trip.getScheduledTime(),
                trip.getEstimatedPrice() == null ? BigDecimal.ZERO : trip.getEstimatedPrice(),
                trip.getDriver() == null ? null : trip.getDriver().getName(),
                trip.getVehicle() == null
                        ? null
                        : trip.getVehicle().getBrand() + " " + trip.getVehicle().getModel(),
                trip.getRoute() == null ? null : trip.getRoute().getDistanceKm(),
                trip.getRoute() == null ? null : trip.getRoute().getEstimatedDurationMin());
    }

    public boolean canCancel() {
        return status == TripStatus.PENDING || status == TripStatus.ACCEPTED;
    }

    public String routeLabel() {
        return valueOrFallback(originAddress, "Origem")
                + " -> "
                + valueOrFallback(destinationAddress, "Destino");
    }

    public String originDisplay() {
        return valueOrFallback(originAddress, "Origem");
    }

    public String destinationDisplay() {
        return valueOrFallback(destinationAddress, "Destino");
    }

    public String vehicleCategoryLabel() {
        return valueOrFallback(vehicleCategory, "STANDARD").toUpperCase();
    }

    public String driverDisplay() {
        return valueOrFallback(driverName, "A confirmar");
    }

    public String scheduleDayLabel() {
        if (scheduledTime == null) {
            return "-";
        }
        LocalDate date = scheduledTime.toLocalDate();
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "Hoje";
        }
        if (date.equals(today.plusDays(1))) {
            return "Amanh\u00E3";
        }
        return DAY_MONTH_FORMATTER.format(date);
    }

    public String scheduleDateLabel() {
        if (scheduledTime == null) {
            return "-";
        }
        return DateTimeFormatter.ofPattern("d MMM yyyy", PT_LOCALE).format(scheduledTime);
    }

    public String scheduleTimeLabel() {
        if (scheduledTime == null) {
            return "-";
        }
        return scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String scheduleHeadline() {
        if (scheduledTime == null) {
            return "Data a confirmar";
        }
        return scheduleDayLabel() + ", " + scheduleTimeLabel();
    }

    public String distanceLabel() {
        if (distanceKm == null) {
            return "Dist\u00E2ncia indispon\u00EDvel";
        }
        return String.format(PT_LOCALE, "%.1f km", distanceKm);
    }

    public String durationLabel() {
        if (durationMin == null) {
            return "Dura\u00E7\u00E3o indispon\u00EDvel";
        }
        return durationMin + " min";
    }

    public String statusLabel() {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "A confirmar";
            case ACCEPTED -> "Agendada";
            case IN_PROGRESS -> "Em curso";
            case COMPLETED -> "Conclu\u00EDda";
            case CANCELLED -> "Cancelada";
            case REJECTED -> "Rejeitada";
        };
    }

    public String statusClass() {
        if (status == TripStatus.ACCEPTED) {
            return "is-confirmed";
        }
        if (status == TripStatus.CANCELLED || status == TripStatus.REJECTED) {
            return "is-cancelled";
        }
        if (status == TripStatus.COMPLETED) {
            return "is-completed";
        }
        return "is-pending";
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
