package com.obar.web.maps;

import java.math.BigDecimal;

public record ActiveTripResponse(
        Integer tripId,
        Integer routeId,
        String status,
        String originAddress,
        String destinationAddress,
        double originLat,
        double originLng,
        double destinationLat,
        double destinationLng,
        double distanceKm,
        int durationMin,
        BigDecimal estimatedPrice) {
}
