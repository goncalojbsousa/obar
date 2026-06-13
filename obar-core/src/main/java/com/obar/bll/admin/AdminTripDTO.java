package com.obar.bll.admin;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTripDTO(
        Integer id,
        Integer clientId,
        String clientName,
        Integer driverId,
        String driverName,
        Integer vehicleId,
        String vehicleBrand,
        String vehicleModel,
        String vehicleLicensePlate,
        String vehicleCategory,
        TripStatus status,
        TripType tripType,
        BigDecimal estimatedPrice,
        BigDecimal finalPrice,
        Float distanceKm,
        LocalDateTime requestTime,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String cancelledBy,
        String cancelReason,
        String notes,
        String originAddress,
        String destinationAddress) {

    public static AdminTripDTO from(Trip trip) {
        return new AdminTripDTO(
                trip.getId(),
                trip.getClient() == null ? null : trip.getClient().getId(),
                trip.getClient() == null ? null : trip.getClient().getName(),
                trip.getDriver() == null ? null : trip.getDriver().getId(),
                trip.getDriver() == null ? null : trip.getDriver().getName(),
                trip.getVehicle() == null ? null : trip.getVehicle().getId(),
                trip.getVehicle() == null ? null : trip.getVehicle().getBrand(),
                trip.getVehicle() == null ? null : trip.getVehicle().getModel(),
                trip.getVehicle() == null ? null : trip.getVehicle().getLicensePlate(),
                trip.getVehicle() == null ? null : trip.getVehicle().getCategory(),
                trip.getStatus(),
                trip.getTripType(),
                trip.getEstimatedPrice(),
                trip.getFinalPrice(),
                trip.getRoute() == null ? null : trip.getRoute().getDistanceKm(),
                trip.getRequestTime(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getCancelledBy(),
                trip.getCancelReason(),
                trip.getNotes(),
                trip.getRoute() == null ? null : trip.getRoute().getOriginAddress(),
                trip.getRoute() == null ? null : trip.getRoute().getDestinationAddress());
    }

    public String vehicleDisplay() {
        if (vehicleId == null) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("#").append(vehicleId);
        if (vehicleBrand != null && !vehicleBrand.isBlank()) {
            builder.append(" | ").append(vehicleBrand);
        }
        if (vehicleModel != null && !vehicleModel.isBlank()) {
            builder.append(" ").append(vehicleModel);
        }
        if (vehicleLicensePlate != null && !vehicleLicensePlate.isBlank()) {
            builder.append(" | ").append(vehicleLicensePlate);
        }
        return builder.toString();
    }
}
