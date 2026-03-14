package com.obar.dal;

import com.obar.model.Vehicle;
import org.hibernate.Session;

import java.util.List;

public class VehicleRepository extends BaseRepository<Vehicle, Integer> {

    public VehicleRepository() {
        super(Vehicle.class);
    }

    public List<Vehicle> findByDriverId(Integer driverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Vehicle v WHERE v.driver.id = :driverId AND v.active = true", Vehicle.class)
                    .setParameter("driverId", driverId)
                    .list();
        }
    }
}