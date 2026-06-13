package com.obar.web.client;

import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ClientViews {

    private ClientViews() {
    }

    public record ClientProfileView(
            String name,
            String initials,
            String photoUrl,
            String email,
            String phone,
            String taxNumber,
            String accountStatus,
            String memberSince,
            String averageRating,
            int reviewCount,
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

        public static ClientProfileView from(User user, List<Trip> trips, int reviewCount) {
            List<Trip> safeTrips = trips == null ? List.of() : trips;
            Trip lastTrip = safeTrips.stream()
                    .max(Comparator.comparing(ClientProfileView::tripReferenceTime,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);

            return new ClientProfileView(
                    valueOrFallback(user.getName(), "-"),
                    initials(user.getName()),
                    user.getPhotoUrl(),
                    valueOrFallback(user.getEmail(), "-"),
                    valueOrFallback(user.getPhone(), "-"),
                    valueOrFallback(user.getTaxNumber(), "-"),
                    statusLabel(user),
                    formatMonth(user.getCreatedAt()),
                    String.format(Locale.forLanguageTag("pt-PT"), "%.1f",
                            user.getAverageRating() == null ? 0f : user.getAverageRating()),
                    reviewCount,
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
            return trip.getScheduledTime() == null ? trip.getRequestTime() : trip.getScheduledTime();
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
                case COMPLETED -> "Conclu\u00EDda";
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
    }

    public record RecentTripView(
            Integer id,
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
            return referenceTime == null ? "-" : DateTimeFormatter.ofPattern("d MMM yyyy", PT_LOCALE)
                    .format(referenceTime);
        }

        public String timeLabel() {
            return referenceTime == null ? "-" : referenceTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        public String distanceLabel() {
            return distanceKm == null ? "-" : String.format(PT_LOCALE, "%.1f km", distanceKm);
        }

        public String statusLabel() {
            if (status == null) {
                return "-";
            }
            return switch (status) {
                case COMPLETED -> "Concluida";
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
            return trip.getScheduledTime() == null ? trip.getRequestTime() : trip.getScheduledTime();
        }

        private static BigDecimal price(Trip trip) {
            if (trip.getFinalPrice() != null) {
                return trip.getFinalPrice();
            }
            return trip.getEstimatedPrice() == null ? BigDecimal.ZERO : trip.getEstimatedPrice();
        }

    }

    public record ScheduledTripsPageView(
            List<ScheduledTripView> scheduledTrips,
            List<RecentTripView> recentTrips,
            int activeScheduledCount,
            int nextDayCount,
            BigDecimal expectedTotal,
            BigDecimal nextDayExpectedTotal,
            String mostUsedCategory,
            ScheduledTripView nextDayTrip,
            int recentPage,
            int recentTotalPages) {

        private static final int RECENT_PAGE_SIZE = 3;

        public static ScheduledTripsPageView from(List<Trip> scheduledTrips, List<Trip> allTrips,
                int requestedRecentPage) {
            List<Trip> safeScheduledTrips = scheduledTrips == null ? List.of() : scheduledTrips;
            List<Trip> safeAllTrips = allTrips == null ? List.of() : allTrips;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tomorrow = now.plusHours(24);

            List<ScheduledTripView> activeScheduled = safeScheduledTrips.stream()
                    .map(ScheduledTripView::from)
                    .filter(ScheduledTripView::canCancel)
                    .sorted(Comparator.comparing(ScheduledTripView::scheduledTime,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            List<RecentTripView> allRecentTrips = safeAllTrips.stream()
                    .filter(ScheduledTripsPageView::isRecentTrip)
                    .sorted(Comparator.comparing(ScheduledTripsPageView::tripReferenceTime,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(RecentTripView::from)
                    .toList();

            int recentTotalCount = allRecentTrips.size();
            int recentTotalPages = Math.max(1, (int) Math.ceil((double) recentTotalCount / RECENT_PAGE_SIZE));
            int recentPage = Math.max(1, Math.min(requestedRecentPage, recentTotalPages));
            int recentStart = Math.min((recentPage - 1) * RECENT_PAGE_SIZE, recentTotalCount);
            List<RecentTripView> recentTrips = allRecentTrips.subList(recentStart,
                    Math.min(recentStart + RECENT_PAGE_SIZE, recentTotalCount));

            BigDecimal expectedTotal = activeScheduled.stream()
                    .map(ScheduledTripView::estimatedPrice)
                    .filter(price -> price != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<ScheduledTripView> nextDayTrips = activeScheduled.stream()
                    .filter(trip -> trip.scheduledTime() != null)
                    .filter(trip -> !trip.scheduledTime().isBefore(now) && trip.scheduledTime().isBefore(tomorrow))
                    .toList();

            BigDecimal nextDayExpectedTotal = nextDayTrips.stream()
                    .map(ScheduledTripView::estimatedPrice)
                    .filter(price -> price != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new ScheduledTripsPageView(
                    activeScheduled,
                    recentTrips,
                    activeScheduled.size(),
                    nextDayTrips.size(),
                    expectedTotal,
                    nextDayExpectedTotal,
                    mostUsedCategory(safeAllTrips),
                    nextDayTrips.isEmpty() ? null : nextDayTrips.get(0),
                    recentPage,
                    recentTotalPages);
        }

        public boolean hasRecentPagination() {
            return recentTotalPages > 1;
        }

        public boolean hasPreviousRecentPage() {
            return recentPage > 1;
        }

        public boolean hasNextRecentPage() {
            return recentPage < recentTotalPages;
        }

        public int previousRecentPage() {
            return Math.max(1, recentPage - 1);
        }

        public int nextRecentPage() {
            return Math.min(recentTotalPages, recentPage + 1);
        }

        private static boolean isRecentTrip(Trip trip) {
            return trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED;
        }

        private static String mostUsedCategory(List<Trip> trips) {
            return trips.stream()
                    .map(Trip::getVehicleCategory)
                    .filter(category -> category != null && !category.isBlank())
                    .collect(Collectors.groupingBy(String::toUpperCase, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");
        }

        private static LocalDateTime tripReferenceTime(Trip trip) {
            if (trip.getEndTime() != null) {
                return trip.getEndTime();
            }
            return trip.getScheduledTime() == null ? trip.getRequestTime() : trip.getScheduledTime();
        }
    }

    public record ScheduledTripView(
            Integer id,
            String originAddress,
            String destinationAddress,
            String vehicleCategory,
            TripStatus status,
            LocalDateTime scheduledTime,
            BigDecimal estimatedPrice,
            String driverName,
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
                    trip.getRoute() == null ? null : trip.getRoute().getDistanceKm(),
                    trip.getRoute() == null ? null : trip.getRoute().getEstimatedDurationMin());
        }

        public boolean canCancel() {
            return status == TripStatus.PENDING || status == TripStatus.ACCEPTED;
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
            return scheduledTime == null ? "-" : DateTimeFormatter.ofPattern("d MMM yyyy", PT_LOCALE)
                    .format(scheduledTime);
        }

        public String scheduleTimeLabel() {
            return scheduledTime == null ? "-" : scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        public String scheduleHeadline() {
            return scheduledTime == null ? "Data a confirmar" : scheduleDayLabel() + ", " + scheduleTimeLabel();
        }

        public String distanceLabel() {
            return distanceKm == null ? "Dist\u00E2ncia indispon\u00EDvel"
                    : String.format(PT_LOCALE, "%.1f km", distanceKm);
        }

        public String durationLabel() {
            return durationMin == null ? "Dura\u00E7\u00E3o indispon\u00EDvel" : durationMin + " min";
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
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
