package com.obar.bll.admin;

import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;

import java.math.BigDecimal;

/**
 * Input command used by admin screens to create trips.
 */
public record AdminTripCommand(
        Integer clientId,
        Integer driverId,
        String originAddress,
        String destinationAddress,
        TripType tripType,
        TripStatus status,
        String notes,
        BigDecimal estimatedPrice,
        BigDecimal finalPrice) {
}
