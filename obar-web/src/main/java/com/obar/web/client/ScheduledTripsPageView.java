package com.obar.web.client;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ScheduledTripsPageView(
        List<ScheduledTripView> scheduledTrips,
        List<RecentTripView> recentTrips,
        int activeScheduledCount,
        int nextDayCount,
        BigDecimal expectedTotal,
        BigDecimal nextDayExpectedTotal,
        String mostUsedCategory,
        ScheduledTripView nextDayTrip,
        ScheduledTripView nextTrip,
        int recentPage,
        int recentTotalPages,
        int recentTotalCount) {

    private static final int RECENT_PAGE_SIZE = 3;

    public static ScheduledTripsPageView from(List<Trip> scheduledTrips, List<Trip> allTrips) {
        return from(scheduledTrips, allTrips, 1);
    }

    public static ScheduledTripsPageView from(List<Trip> scheduledTrips, List<Trip> allTrips, int requestedRecentPage) {
        List<Trip> safeScheduledTrips = scheduledTrips == null ? List.of() : scheduledTrips;
        List<Trip> safeAllTrips = allTrips == null ? List.of() : allTrips;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusHours(24);

        List<ScheduledTripView> scheduledViews = safeScheduledTrips.stream()
                .map(ScheduledTripView::from)
                .sorted(Comparator.comparing(ScheduledTripView::scheduledTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ScheduledTripView> activeScheduled = scheduledViews.stream()
                .filter(ScheduledTripView::canCancel)
                .toList();

        ScheduledTripView nextTrip = activeScheduled.stream()
                .filter(trip -> trip.scheduledTime() != null)
                .min(Comparator.comparing(ScheduledTripView::scheduledTime))
                .orElse(null);

        List<RecentTripView> allRecentTrips = safeAllTrips.stream()
                .filter(ScheduledTripsPageView::isRecentTrip)
                .sorted(Comparator.comparing(ScheduledTripsPageView::tripReferenceTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(RecentTripView::from)
                .toList();

        int recentTotalCount = allRecentTrips.size();
        int recentTotalPages = Math.max(1, (int) Math.ceil((double) recentTotalCount / RECENT_PAGE_SIZE));
        int recentPage = clamp(requestedRecentPage, 1, recentTotalPages);
        int recentStart = Math.min((recentPage - 1) * RECENT_PAGE_SIZE, recentTotalCount);
        int recentEnd = Math.min(recentStart + RECENT_PAGE_SIZE, recentTotalCount);
        List<RecentTripView> recentTrips = allRecentTrips.subList(recentStart, recentEnd);

        BigDecimal expectedTotal = activeScheduled.stream()
                .map(ScheduledTripView::estimatedPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ScheduledTripView> nextDayTrips = activeScheduled.stream()
                .filter(trip -> trip.scheduledTime() != null)
                .filter(trip -> !trip.scheduledTime().isBefore(now) && trip.scheduledTime().isBefore(tomorrow))
                .toList();

        long nextDayCount = activeScheduled.stream()
                .filter(trip -> trip.scheduledTime() != null)
                .filter(trip -> !trip.scheduledTime().isBefore(now) && trip.scheduledTime().isBefore(tomorrow))
                .count();

        BigDecimal nextDayExpectedTotal = nextDayTrips.stream()
                .map(ScheduledTripView::estimatedPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ScheduledTripsPageView(
                activeScheduled,
                recentTrips,
                activeScheduled.size(),
                Math.toIntExact(nextDayCount),
                expectedTotal,
                nextDayExpectedTotal,
                mostUsedCategory(safeAllTrips),
                nextDayTrips.isEmpty() ? null : nextDayTrips.get(0),
                nextTrip,
                recentPage,
                recentTotalPages,
                recentTotalCount);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
        if (trip.getScheduledTime() != null) {
            return trip.getScheduledTime();
        }
        return trip.getRequestTime();
    }
}
