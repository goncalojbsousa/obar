package com.obar.web.maps;

import java.math.BigDecimal;

public record TripRequestResponse(
                Integer tripId,
                Integer routeId,
                double distanceKm,
                int durationMin,
                BigDecimal estimatedPrice,
                String status) {
}
