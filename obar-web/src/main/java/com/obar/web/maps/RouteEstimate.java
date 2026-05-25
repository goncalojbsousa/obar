package com.obar.web.maps;

import java.math.BigDecimal;

public record RouteEstimate(
                double distanceKm,
                int durationMin,
                BigDecimal estimatedPrice,
                Object geometry) {
}
