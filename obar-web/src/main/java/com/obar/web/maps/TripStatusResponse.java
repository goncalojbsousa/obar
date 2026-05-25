package com.obar.web.maps;

public record TripStatusResponse(
                Integer tripId,
                String status,
                String message) {
}
