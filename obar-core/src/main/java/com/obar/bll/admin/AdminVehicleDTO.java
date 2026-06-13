package com.obar.bll.admin;

import com.obar.model.User;
import com.obar.model.Vehicle;

import java.math.BigDecimal;

public record AdminVehicleDTO(
        Integer id,
        Integer driverId,
        String driverName,
        String brand,
        String model,
        String color,
        String licensePlate,
        Integer year,
        String category,
        BigDecimal baseFare,
        BigDecimal pricePerKm,
        Boolean active) {

    public static AdminVehicleDTO from(Vehicle vehicle) {
        User driver = vehicle.getDriver();
        return new AdminVehicleDTO(
                vehicle.getId(),
                driver == null ? null : driver.getId(),
                driver == null ? null : driver.getName(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getLicensePlate(),
                vehicle.getYear(),
                vehicle.getCategory(),
                vehicle.getBaseFare(),
                vehicle.getPricePerKm(),
                vehicle.getActive());
    }

    public String vehicleName() {
        String value = text(brand) + " " + text(model);
        return value.isBlank() ? "-" : value.trim();
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
