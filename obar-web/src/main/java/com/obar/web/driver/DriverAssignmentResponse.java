package com.obar.web.driver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriverAssignmentResponse(
        Integer tripId,
        String clientName,
        Float clientRating,
        String originAddress,
        String destinationAddress,
        double originLat,
        double originLng,
        double destinationLat,
        double destinationLng,
        double distanceKm,
        int durationMin,
        BigDecimal estimatedPrice,
        String vehicleCategory,
        String status,
        LocalDateTime assignedAt,
        long secondsLeft,
        String routeMode,
        Object geometry) {
}
