package com.obar.bll;

import com.obar.dal.UserRepository;
import com.obar.dal.VehicleRepository;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.UserType;

import java.util.List;
import java.util.Optional;

public class VehicleService {

    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final UserRepository userRepository = new UserRepository();

    public Vehicle addVehicle(Vehicle vehicle) {
        User driver = vehicle.getDriver();
        if (driver == null || driver.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("Veículo tem de ser associado a um condutor.");
        }
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> findByDriver(Integer driverId) {
        return vehicleRepository.findByDriverId(driverId);
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
}