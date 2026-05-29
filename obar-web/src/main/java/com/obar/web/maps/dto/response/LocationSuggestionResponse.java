package com.obar.web.maps.dto.response;

public record LocationSuggestionResponse(
        String label,
        double lat,
        double lng) {
}
