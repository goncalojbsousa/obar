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
    private final String clientName;
    private final String driverName;
    private final TripStatus status;
    private final TripType tripType;
    private final BigDecimal estimatedPrice;
    private final BigDecimal finalPrice;
    private final Float distanceKm;
    private final LocalDateTime requestTime;
    private final String originAddress;
    private final String destinationAddress;

    private AdminTripDTO(
            Integer id,
            String clientName,
            String driverName,
            TripStatus status,
            TripType tripType,
            BigDecimal estimatedPrice,
            BigDecimal finalPrice,
            Float distanceKm,
            LocalDateTime requestTime,
            String originAddress,
            String destinationAddress) {
        this.id = id;
        this.clientName = clientName;
        this.driverName = driverName;
        this.status = status;
        this.tripType = tripType;
        this.estimatedPrice = estimatedPrice;
        this.finalPrice = finalPrice;
        this.distanceKm = distanceKm;
        this.requestTime = requestTime;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
    }

    public static AdminTripDTO from(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip must not be null.");
        }

        return new AdminTripDTO(
                trip.getId(),
                trip.getClient() == null ? null : trip.getClient().getName(),
                trip.getDriver() == null ? null : trip.getDriver().getName(),
                trip.getStatus(),
                trip.getTripType(),
                trip.getEstimatedPrice(),
                trip.getFinalPrice(),
                trip.getRoute() == null ? null : trip.getRoute().getDistanceKm(),
                trip.getRequestTime(),
                trip.getRoute() == null ? null : trip.getRoute().getOriginAddress(),
                trip.getRoute() == null ? null : trip.getRoute().getDestinationAddress());
    }

    public Integer getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public String getDriverName() {
        return driverName;
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

    public String getOriginAddress() {
        return originAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }
}
