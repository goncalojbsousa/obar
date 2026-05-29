package com.obar.web.maps.dto.response;

public record TripCancellationResponse(
        Integer tripId,
        String status,
        String message) {
}
