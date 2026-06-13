package com.obar.dal;

import com.obar.model.Vehicle;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

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

    public Optional<Vehicle> findByLicensePlate(String licensePlate) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Vehicle v WHERE upper(v.licensePlate) = upper(:licensePlate)", Vehicle.class)
                    .setParameter("licensePlate", licensePlate)
                    .uniqueResultOptional();
        }
    }

    public Optional<Vehicle> findActiveByDriverIdAndCategory(Integer driverId, String category) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Vehicle v "
                            + "WHERE v.driver.id = :driverId "
                            + "AND v.active = true "
                            + "AND upper(v.category) = upper(:category)",
                    Vehicle.class)
                    .setParameter("driverId", driverId)
                    .setParameter("category", category)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        }
    }

    public List<String> findActiveCategories() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT DISTINCT upper(v.category) "
                            + "FROM Vehicle v "
                            + "WHERE v.active = true "
                            + "ORDER BY upper(v.category)",
                    String.class)
                    .list();
        }
    }
}
