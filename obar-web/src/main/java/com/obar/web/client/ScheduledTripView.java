package com.obar.web.client;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScheduledTripView(
        Integer id,
        String originAddress,
        String destinationAddress,
        String vehicleCategory,
        TripStatus status,
        LocalDateTime scheduledTime,
        BigDecimal estimatedPrice,
        String driverName,
        String vehicleDisplay) {

    public static ScheduledTripView from(Trip trip) {
        return new ScheduledTripView(
                trip.getId(),
                trip.getRoute().getOriginAddress(),
                trip.getRoute().getDestinationAddress(),
                trip.getVehicleCategory(),
                trip.getStatus(),
                trip.getScheduledTime(),
                trip.getEstimatedPrice(),
                trip.getDriver() == null ? null : trip.getDriver().getName(),
                trip.getVehicle() == null
                        ? null
                        : trip.getVehicle().getBrand() + " " + trip.getVehicle().getModel());
    }

    public boolean canCancel() {
        return status == TripStatus.PENDING || status == TripStatus.ACCEPTED;
    }
}
