package com.obar.dal;

import com.obar.model.Vehicle;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class VehicleRepository extends BaseRepository<Vehicle, Integer> {

    public VehicleRepository() {
        super(Vehicle.class);
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        Transaction tx = null;
        boolean shouldActivate = vehicle != null && Boolean.TRUE.equals(vehicle.getActive());
        try (Session session = getSession()) {
            if (shouldActivate) {
                vehicle.setActive(false);
            }
            tx = session.beginTransaction();
            session.persist(vehicle);
            session.flush();
            activateVehicleAfterDeactivatingOthers(session, vehicle, shouldActivate);
            tx.commit();
            return vehicle;
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            if (shouldActivate && vehicle != null) {
                vehicle.setActive(true);
            }
            throw e;
        }
    }

    @Override
    public Vehicle update(Vehicle vehicle) {
        Transaction tx = null;
        boolean shouldActivate = vehicle != null && Boolean.TRUE.equals(vehicle.getActive());
        try (Session session = getSession()) {
            if (shouldActivate) {
                vehicle.setActive(false);
            }
            tx = session.beginTransaction();
            Vehicle merged = session.merge(vehicle);
            session.flush();
            activateVehicleAfterDeactivatingOthers(session, merged, shouldActivate);
            if (shouldActivate) {
                vehicle.setActive(true);
            }
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            if (shouldActivate && vehicle != null) {
                vehicle.setActive(true);
            }
            throw e;
        }
    }

    public List<Vehicle> findByDriverId(Integer driverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Vehicle v "
                            + "WHERE v.driver.id = :driverId "
                            + "AND v.active = true "
                            + "AND v.removed = false",
                    Vehicle.class)
                    .setParameter("driverId", driverId)
                    .list();
        }
    }

    public List<Vehicle> findAllByDriverId(Integer driverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Vehicle v "
                            + "WHERE v.driver.id = :driverId "
                            + "AND v.removed = false "
                            + "ORDER BY v.id",
                    Vehicle.class)
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
                            + "AND v.removed = false "
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
                            + "AND v.removed = false "
                            + "ORDER BY upper(v.category)",
                    String.class)
                    .list();
        }
    }

    private void activateVehicleAfterDeactivatingOthers(Session session, Vehicle vehicle, boolean shouldActivate) {
        if (vehicle == null
                || !shouldActivate
                || vehicle.getId() == null
                || vehicle.getDriver() == null
                || vehicle.getDriver().getId() == null) {
            return;
        }

        session.createMutationQuery(
                "UPDATE Vehicle v "
                        + "SET v.active = false "
                        + "WHERE v.driver.id = :driverId "
                        + "AND v.active = true "
                        + "AND v.removed = false "
                        + "AND v.id <> :vehicleId")
                .setParameter("driverId", vehicle.getDriver().getId())
                .setParameter("vehicleId", vehicle.getId())
                .executeUpdate();
        vehicle.setActive(true);
        session.flush();
    }
}
