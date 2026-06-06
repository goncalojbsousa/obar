package com.obar.bll.admin;

import com.obar.model.User;
import com.obar.model.Vehicle;

import java.math.BigDecimal;

/**
 * Read-only BLL DTO exposed to UI for admin vehicle management screens.
 */
public final class AdminVehicleDTO {

    private final Integer id;
    private final Integer driverId;
    private final String driverName;
    private final String brand;
    private final String model;
    private final String color;
    private final String licensePlate;
    private final Integer year;
    private final String category;
    private final BigDecimal baseFare;
    private final BigDecimal pricePerKm;
    private final Boolean active;

    private AdminVehicleDTO(
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
        this.id = id;
        this.driverId = driverId;
        this.driverName = driverName;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.licensePlate = licensePlate;
        this.year = year;
        this.category = category;
        this.baseFare = baseFare;
        this.pricePerKm = pricePerKm;
        this.active = active;
    }

    public static AdminVehicleDTO from(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle must not be null.");
        }

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

    public Integer getId() {
        return id;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public Integer getYear() {
        return year;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public BigDecimal getPricePerKm() {
        return pricePerKm;
    }

    public Boolean getActive() {
        return active;
    }

    public String getVehicleName() {
        String safeBrand = AdminTextSanitizer.safe(brand);
        String safeModel = AdminTextSanitizer.safe(model);
        String vehicleName = AdminTextSanitizer.safe(safeBrand + " " + safeModel);
        return vehicleName.isBlank() ? "-" : vehicleName;
    }
}
