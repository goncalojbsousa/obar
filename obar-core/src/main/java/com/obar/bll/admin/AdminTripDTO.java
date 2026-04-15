package com.obar.bll.admin;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only BLL DTO exposed to UI for admin trip dashboard screens.
 */
public final class AdminTripDTO {

    private final Integer id;
    private final Integer clientId;
    private final String clientName;
    private final Integer driverId;
    private final String driverName;
    private final Integer vehicleId;
    private final String vehicleBrand;
    private final String vehicleModel;
    private final String vehicleLicensePlate;
    private final String vehicleCategory;
    private final TripStatus status;
    private final TripType tripType;
    private final BigDecimal estimatedPrice;
    private final BigDecimal finalPrice;
    private final Float distanceKm;
    private final LocalDateTime requestTime;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String cancelledBy;
    private final String cancelReason;
    private final String notes;
    private final String originAddress;
    private final String destinationAddress;

    private AdminTripDTO(
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
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.driverId = driverId;
        this.driverName = driverName;
        this.vehicleId = vehicleId;
        this.vehicleBrand = vehicleBrand;
        this.vehicleModel = vehicleModel;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleCategory = vehicleCategory;
        this.status = status;
        this.tripType = tripType;
        this.estimatedPrice = estimatedPrice;
        this.finalPrice = finalPrice;
        this.distanceKm = distanceKm;
        this.requestTime = requestTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.notes = notes;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
    }

    public static AdminTripDTO from(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip must not be null.");
        }

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

    public Integer getId() {
        return id;
    }

    public Integer getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public String getVehicleLicensePlate() {
        return vehicleLicensePlate;
    }

    public String getVehicleCategory() {
        return vehicleCategory;
    }

    public TripStatus getStatus() {
        return status;
    }

    public TripType getTripType() {
        return tripType;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public Float getDistanceKm() {
        return distanceKm;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public String getNotes() {
        return notes;
    }

    public String getOriginAddress() {
        return originAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public String getVehicleDisplay() {
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
