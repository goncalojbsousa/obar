package com.obar.web.maps;

public record RouteEstimateRequest(
                double originLat,
                double originLng,
                double destinationLat,
                double destinationLng,
                String originAddress,
                String destinationAddress) {
}
