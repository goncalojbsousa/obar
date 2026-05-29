package com.obar.web.maps.dto.response;

import java.math.BigDecimal;

public record RouteEstimateResponse(
        double distanceKm,
        int durationMin,
        BigDecimal estimatedPrice,
        Object geometry) {
}
