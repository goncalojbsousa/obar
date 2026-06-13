package com.obar.dal;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.List;

public class TripRepository extends BaseRepository<Trip, Integer> {

    public TripRepository() {
        super(Trip.class);
    }

    public List<Trip> findByClientId(Integer clientId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.route "
                            + "LEFT JOIN FETCH t.driver "
                            + "LEFT JOIN FETCH t.vehicle "
                            + "WHERE t.client.id = :clientId "
                            + "ORDER BY t.requestTime DESC",
                    Trip.class)
                    .setParameter("clientId", clientId)
                    .list();
        }
    }

    public List<Trip> findScheduledByClientId(Integer clientId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.route "
                            + "LEFT JOIN FETCH t.driver "
                            + "LEFT JOIN FETCH t.vehicle "
                            + "WHERE t.client.id = :clientId "
                            + "AND t.tripType = :tripType "
                            + "ORDER BY t.scheduledTime ASC, t.requestTime DESC",
                    Trip.class)
                    .setParameter("clientId", clientId)
                    .setParameter("tripType", TripType.SCHEDULED)
                    .list();
        }
    }

    public List<Trip> findByDriverId(Integer driverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Trip t WHERE t.driver.id = :driverId", Trip.class)
                    .setParameter("driverId", driverId)
                    .list();
        }
    }

    public List<Trip> findByStatus(TripStatus status) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Trip t WHERE t.status = :status", Trip.class)
                    .setParameter("status", status)
                    .list();
        }
    }

    public List<Trip> findActiveImmediateTripsWithRoute() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.client "
                            + "JOIN FETCH t.route "
                            + "LEFT JOIN FETCH t.driver "
                            + "WHERE t.tripType = :tripType "
                            + "AND t.status IN (:statuses) "
                            + "ORDER BY t.requestTime DESC",
                    Trip.class)
                    .setParameter("tripType", TripType.IMMEDIATE)
                    .setParameter("statuses", List.of(
                            TripStatus.PENDING,
                            TripStatus.ACCEPTED,
                            TripStatus.IN_PROGRESS))
                    .list();
        }
    }

    public List<Trip> findPendingByVehicleCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM Trip t "
                            + "WHERE t.status = :status "
                            + "AND upper(t.vehicleCategory) IN (:categories) "
                            + "ORDER BY t.requestTime ASC",
                    Trip.class)
                    .setParameter("status", TripStatus.PENDING)
                    .setParameter("categories", categories.stream()
                            .map(String::toUpperCase)
                            .toList())
                    .list();
        }
    }

    public List<Trip> findPendingScheduledByVehicleCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.client "
                            + "JOIN FETCH t.route "
                            + "WHERE t.tripType = :tripType "
                            + "AND t.status = :status "
                            + "AND upper(t.vehicleCategory) IN (:categories) "
                            + "AND t.scheduledTime > :now "
                            + "ORDER BY t.scheduledTime ASC",
                    Trip.class)
                    .setParameter("tripType", TripType.SCHEDULED)
                    .setParameter("status", TripStatus.PENDING)
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("categories", categories.stream()
                            .map(String::toUpperCase)
                            .toList())
                    .list();
        }
    }

    public List<Trip> findAcceptedScheduledByDriverId(Integer driverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.client "
                            + "JOIN FETCH t.route "
                            + "LEFT JOIN FETCH t.vehicle "
                            + "WHERE t.driver.id = :driverId "
                            + "AND t.tripType = :tripType "
                            + "AND t.status = :status "
                            + "ORDER BY t.scheduledTime ASC",
                    Trip.class)
                    .setParameter("driverId", driverId)
                    .setParameter("tripType", TripType.SCHEDULED)
                    .setParameter("status", TripStatus.ACCEPTED)
                    .list();
        }
    }

    public boolean hasAcceptedScheduledTripStartingBefore(Integer driverId, LocalDateTime limit) {
        try (Session session = getSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(t.id) FROM Trip t "
                            + "WHERE t.driver.id = :driverId "
                            + "AND t.tripType = :tripType "
                            + "AND t.status = :status "
                            + "AND t.scheduledTime <= :limit",
                    Long.class)
                    .setParameter("driverId", driverId)
                    .setParameter("tripType", TripType.SCHEDULED)
                    .setParameter("status", TripStatus.ACCEPTED)
                    .setParameter("limit", limit)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public List<Trip> findAllForAdminDashboard() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "LEFT JOIN FETCH t.client "
                            + "LEFT JOIN FETCH t.driver "
                            + "LEFT JOIN FETCH t.route "
                            + "ORDER BY t.requestTime DESC",
                    Trip.class)
                    .list();
        }
    }
}
