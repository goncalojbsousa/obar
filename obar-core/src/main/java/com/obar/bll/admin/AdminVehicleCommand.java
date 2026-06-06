package com.obar.bll.admin;

import java.math.BigDecimal;

/**
 * Input data used by the admin vehicle management use cases.
 */
public record AdminVehicleCommand(
        Integer driverId,
        String brand,
        String model,
        String color,
        String licensePlate,
        Integer year,
        String category,
        BigDecimal baseFare,
        BigDecimal pricePerKm,
        Boolean active) {
}
