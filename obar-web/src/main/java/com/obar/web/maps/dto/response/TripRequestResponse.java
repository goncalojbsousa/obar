package com.obar.web.maps.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripRequestResponse(
                Integer tripId,
                Integer routeId,
                double distanceKm,
                int durationMin,
                BigDecimal estimatedPrice,
                String vehicleCategory,
                String tripType,
                LocalDateTime scheduledAt,
                String status) {
}
