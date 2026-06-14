package com.obar.bll;

import com.obar.dal.VehicleRepository;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.UserType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VehicleService {

    private static final List<String> SUPPORTED_CATEGORIES = List.of("STANDARD", "XL", "PREMIUM");
    private static final Set<String> SUPPORTED_CATEGORY_SET = Set.copyOf(SUPPORTED_CATEGORIES);

    private final VehicleRepository vehicleRepository = new VehicleRepository();

    public Vehicle addVehicle(Vehicle vehicle) {
        User driver = vehicle.getDriver();
        if (driver == null || driver.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("Veículo tem de ser associado a um condutor.");
        }
        vehicle.setRemoved(false);
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> findByDriver(Integer driverId) {
        return vehicleRepository.findByDriverId(driverId);
    }

    public List<Vehicle> findAllByDriver(Integer driverId) {
        return vehicleRepository.findAllByDriverId(driverId);
    }

    public List<String> findActiveCategories() {
        return vehicleRepository.findActiveCategories();
    }

    public List<String> supportedCategories() {
        return SUPPORTED_CATEGORIES;
    }

    public Optional<Vehicle> findByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate);
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Escolhe o tipo de ve\u00EDculo.");
        }

        String normalized = category.trim().toUpperCase();
        if (!SUPPORTED_CATEGORY_SET.contains(normalized)) {
            throw new IllegalArgumentException("Tipo de ve\u00EDculo inv\u00E1lido.");
        }
        return normalized;
    }

    public Optional<Vehicle> findById(Integer id) {
        return vehicleRepository.findById(id);
    }

    public Vehicle update(Vehicle vehicle) {
        return vehicleRepository.update(vehicle);
    }

    public void deactivate(Integer vehicleId) {
        vehicleRepository.findById(vehicleId).ifPresent(v -> {
            v.setActive(false);
            vehicleRepository.update(v);
        });
    }

    public void remove(Integer vehicleId) {
        vehicleRepository.findById(vehicleId).ifPresent(v -> {
            v.setActive(false);
            v.setRemoved(true);
            vehicleRepository.update(v);
        });
    }
}
