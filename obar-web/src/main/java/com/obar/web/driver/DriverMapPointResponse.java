package com.obar.web.driver;

public record DriverMapPointResponse(
                Integer id,
                String name,
                String type,
                double lat,
                double lng) {
}
