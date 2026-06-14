package com.obar.web.driver;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class DriverHistoryViews {

    public static final int PAGE_SIZE = 12;
    private static final Locale PT_LOCALE = Locale.forLanguageTag("pt-PT");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", PT_LOCALE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private DriverHistoryViews() {
    }

    public record HistoryPage(
            List<HistoryTrip> trips,
            int page,
            int totalPages,
            long totalTrips,
            long completedTrips,
            BigDecimal totalEarnings,
            String selectedStatus,
            LocalDate from,
            LocalDate to,
            String clientQuery) {

        public static HistoryPage from(List<Trip> trips, int page, int totalPages, long totalTrips,
                long completedTrips, BigDecimal totalEarnings, String selectedStatus,
                LocalDate from, LocalDate to, String clientQuery) {
            return new HistoryPage(
                    trips.stream().map(HistoryTrip::from).toList(),
                    page,
                    totalPages,
                    totalTrips,
                    completedTrips,
                    totalEarnings,
                    selectedStatus == null ? "" : selectedStatus,
                    from,
                    to,
                    clientQuery == null ? "" : clientQuery);
        }

        public boolean hasPagination() {
            return totalPages > 1;
        }

        public boolean hasPreviousPage() {
            return page > 1;
        }

        public boolean hasNextPage() {
            return page < totalPages;
        }

        public int previousPage() {
            return Math.max(1, page - 1);
        }

        public int nextPage() {
            return Math.min(totalPages, page + 1);
        }
    }

    public record HistoryTrip(
            Integer id,
            String dateLabel,
            String timeLabel,
            String clientName,
            String origin,
            String destination,
            String distanceLabel,
            BigDecimal price,
            String statusLabel,
            String statusClass) {

        public static HistoryTrip from(Trip trip) {
            LocalDateTime time = referenceTime(trip);
            return new HistoryTrip(
                    trip.getId(),
                    time == null ? "-" : DATE_FORMATTER.format(time),
                    time == null ? "-" : TIME_FORMATTER.format(time),
                    trip.getClient() == null ? "Cliente indisponivel" : value(trip.getClient().getName(), "Cliente"),
                    trip.getRoute() == null ? "Origem indisponivel"
                            : value(trip.getRoute().getOriginAddress(), "Origem"),
                    trip.getRoute() == null ? "Destino indisponivel"
                            : value(trip.getRoute().getDestinationAddress(), "Destino"),
                    trip.getRoute() == null || trip.getRoute().getDistanceKm() == null
                            ? "-"
                            : String.format(PT_LOCALE, "%.1f km", trip.getRoute().getDistanceKm()),
                    DriverHistoryViews.price(trip),
                    DriverHistoryViews.statusLabel(trip.getStatus()),
                    DriverHistoryViews.statusClass(trip.getStatus()));
        }
    }

    private static LocalDateTime referenceTime(Trip trip) {
        if (trip.getEndTime() != null) {
            return trip.getEndTime();
        }
        if (trip.getStartTime() != null) {
            return trip.getStartTime();
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
        return trip.getEstimatedPrice() == null ? BigDecimal.ZERO : trip.getEstimatedPrice();
    }

    private static String statusLabel(TripStatus status) {
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

    private static String statusClass(TripStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case COMPLETED -> " is-completed";
            case ACCEPTED -> " is-confirmed";
            case IN_PROGRESS -> " is-pending";
            case CANCELLED, REJECTED -> " is-cancelled";
            case PENDING -> " is-pending";
        };
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
