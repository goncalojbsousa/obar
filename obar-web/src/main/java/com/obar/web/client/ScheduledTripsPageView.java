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
        ScheduledTripView nextTrip) {

    public static ScheduledTripsPageView from(List<Trip> scheduledTrips, List<Trip> allTrips) {
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

        List<RecentTripView> recentTrips = safeAllTrips.stream()
                .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                .sorted(Comparator.comparing(ScheduledTripsPageView::tripReferenceTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(RecentTripView::from)
                .toList();

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
                scheduledViews,
                recentTrips,
                activeScheduled.size(),
                Math.toIntExact(nextDayCount),
                expectedTotal,
                nextDayExpectedTotal,
                mostUsedCategory(safeAllTrips),
                nextDayTrips.isEmpty() ? null : nextDayTrips.get(0),
                nextTrip);
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
