package com.obar.web.maps.dto.request;

import java.time.LocalDateTime;

public record RouteEstimateRequest(
                double originLat,
                double originLng,
                double destinationLat,
                double destinationLng,
                String originAddress,
                String destinationAddress,
                String vehicleCategory,
                LocalDateTime scheduledAt) {
}
