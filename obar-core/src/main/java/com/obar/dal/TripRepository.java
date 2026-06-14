package com.obar.dal;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

    public Optional<Trip> findLatestCompletedWithoutClientReview(Integer clientId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.client "
                            + "JOIN FETCH t.driver "
                            + "JOIN FETCH t.route "
                            + "WHERE t.client.id = :clientId "
                            + "AND t.status = :status "
                            + "AND NOT EXISTS ("
                            + "SELECT r.id FROM Review r "
                            + "WHERE r.trip.id = t.id "
                            + "AND r.reviewerType = :reviewerType"
                            + ") "
                            + "ORDER BY t.endTime DESC",
                    Trip.class)
                    .setParameter("clientId", clientId)
                    .setParameter("status", TripStatus.COMPLETED)
                    .setParameter("reviewerType", "CLIENT")
                    .setMaxResults(1)
                    .uniqueResultOptional();
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
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.route "
                            + "JOIN FETCH t.client "
                            + "LEFT JOIN FETCH t.driver "
                            + "LEFT JOIN FETCH t.vehicle "
                            + "WHERE t.driver.id = :driverId "
                            + "ORDER BY t.requestTime DESC",
                    Trip.class)
                    .setParameter("driverId", driverId)
                    .list();
        }
    }

    public List<Trip> findDriverHistory(Integer driverId, TripStatus status, LocalDateTime from,
            LocalDateTime to, String clientQuery, int offset, int limit) {
        try (Session session = getSession()) {
            String filters = driverHistoryFilters(status, from, to, clientQuery);
            var query = session.createQuery(
                    "SELECT t FROM Trip t "
                            + "JOIN FETCH t.route "
                            + "JOIN FETCH t.client c "
                            + "LEFT JOIN FETCH t.vehicle "
                            + "WHERE t.driver.id = :driverId "
                            + filters
                            + "ORDER BY COALESCE(t.endTime, t.startTime, t.scheduledTime, t.requestTime) DESC",
                    Trip.class)
                    .setParameter("driverId", driverId)
                    .setFirstResult(offset)
                    .setMaxResults(limit);
            applyDriverHistoryParameters(query, status, from, to, clientQuery);
            return query.list();
        }
    }

    public long countDriverHistory(Integer driverId, TripStatus status, LocalDateTime from,
            LocalDateTime to, String clientQuery) {
        try (Session session = getSession()) {
            String filters = driverHistoryFilters(status, from, to, clientQuery);
            var query = session.createQuery(
                    "SELECT COUNT(t.id) FROM Trip t "
                            + "JOIN t.client c "
                            + "WHERE t.driver.id = :driverId "
                            + filters,
                    Long.class)
                    .setParameter("driverId", driverId);
            applyDriverHistoryParameters(query, status, from, to, clientQuery);
            return query.uniqueResult();
        }
    }

    public BigDecimal sumCompletedDriverHistory(Integer driverId, LocalDateTime from,
            LocalDateTime to, String clientQuery) {
        try (Session session = getSession()) {
            String filters = driverHistoryFilters(TripStatus.COMPLETED, from, to, clientQuery);
            var query = session.createQuery(
                    "SELECT COALESCE(SUM(COALESCE(t.finalPrice, t.estimatedPrice)), 0) FROM Trip t "
                            + "JOIN t.client c "
                            + "WHERE t.driver.id = :driverId "
                            + filters,
                    BigDecimal.class)
                    .setParameter("driverId", driverId);
            applyDriverHistoryParameters(query, TripStatus.COMPLETED, from, to, clientQuery);
            return query.uniqueResult();
        }
    }

    private String driverHistoryFilters(TripStatus status, LocalDateTime from,
            LocalDateTime to, String clientQuery) {
        StringBuilder filters = new StringBuilder();
        if (status != null) {
            filters.append("AND t.status = :historyStatus ");
        }
        if (from != null) {
            filters.append("AND COALESCE(t.endTime, t.startTime, t.scheduledTime, t.requestTime) >= :historyFrom ");
        }
        if (to != null) {
            filters.append("AND COALESCE(t.endTime, t.startTime, t.scheduledTime, t.requestTime) < :historyTo ");
        }
        if (clientQuery != null && !clientQuery.isBlank()) {
            filters.append("AND LOWER(c.name) LIKE :clientQuery ");
        }
        return filters.toString();
    }

    private void applyDriverHistoryParameters(org.hibernate.query.Query<?> query, TripStatus status,
            LocalDateTime from, LocalDateTime to, String clientQuery) {
        if (status != null) {
            query.setParameter("historyStatus", status);
        }
        if (from != null) {
            query.setParameter("historyFrom", from);
        }
        if (to != null) {
            query.setParameter("historyTo", to);
        }
        if (clientQuery != null && !clientQuery.isBlank()) {
            query.setParameter("clientQuery", "%" + clientQuery.trim().toLowerCase() + "%");
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
