package com.obar.dal;

import com.obar.model.TripDriver;
import com.obar.model.enums.TripDriverStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TripDriverRepository extends BaseRepository<TripDriver, Integer> {

    public TripDriverRepository() {
        super(TripDriver.class);
    }

    public List<TripDriver> findByTripId(Integer tripId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TripDriver td WHERE td.trip.id = :tripId ORDER BY td.assignedAt ASC",
                    TripDriver.class)
                    .setParameter("tripId", tripId)
                    .list();
        }
    }

    public List<Integer> findDriverIdsByTripId(Integer tripId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT td.driver.id FROM TripDriver td WHERE td.trip.id = :tripId",
                    Integer.class)
                    .setParameter("tripId", tripId)
                    .list();
        }
    }

    public Optional<TripDriver> findByTripIdAndDriverId(Integer tripId, Integer driverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TripDriver td WHERE td.trip.id = :tripId AND td.driver.id = :driverId",
                    TripDriver.class)
                    .setParameter("tripId", tripId)
                    .setParameter("driverId", driverId)
                    .uniqueResultOptional();
        }
    }

    public Optional<TripDriver> findCurrentAssignedDriver(Integer tripId) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TripDriver td "
                            + "WHERE td.trip.id = :tripId "
                            + "AND td.status = :status "
                            + "ORDER BY td.assignedAt DESC",
                    TripDriver.class)
                    .setParameter("tripId", tripId)
                    .setParameter("status", TripDriverStatus.ASSIGNED)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        }
    }

    public Optional<TripDriver> findCurrentAssignmentForDriver(Integer driverId, LocalDateTime scheduledActivationLimit) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT td FROM TripDriver td "
                            + "JOIN FETCH td.trip t "
                            + "JOIN FETCH t.client "
                            + "JOIN FETCH t.route "
                            + "JOIN FETCH td.driver "
                            + "WHERE td.driver.id = :driverId "
                            + "AND td.status IN (:assignmentStatuses) "
                            + "AND t.status IN (:tripStatuses) "
                            + "AND (t.tripType = :immediateType "
                            + "OR (t.tripType = :scheduledType "
                            + "AND t.status <> :pendingStatus "
                            + "AND (t.status = :inProgressStatus OR t.scheduledTime <= :activationLimit))) "
                            + "ORDER BY td.assignedAt DESC",
                    TripDriver.class)
                    .setParameter("driverId", driverId)
                    .setParameter("assignmentStatuses", List.of(
                            TripDriverStatus.ASSIGNED,
                            TripDriverStatus.ACCEPTED))
                    .setParameter("tripStatuses", List.of(
                            TripStatus.PENDING,
                            TripStatus.ACCEPTED,
                            TripStatus.IN_PROGRESS))
                    .setParameter("immediateType", TripType.IMMEDIATE)
                    .setParameter("scheduledType", TripType.SCHEDULED)
                    .setParameter("pendingStatus", TripStatus.PENDING)
                    .setParameter("inProgressStatus", TripStatus.IN_PROGRESS)
                    .setParameter("activationLimit", scheduledActivationLimit)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        }
    }

    public List<TripDriver> findExpiredAssignedDrivers(LocalDateTime olderThan) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TripDriver td "
                            + "WHERE td.status = :status "
                            + "AND td.assignedAt <= :olderThan "
                            + "ORDER BY td.assignedAt ASC",
                    TripDriver.class)
                    .setParameter("status", TripDriverStatus.ASSIGNED)
                    .setParameter("olderThan", olderThan)
                    .list();
        }
    }

    public void updateAssignedDriverStatus(Integer tripId, Integer driverId, TripDriverStatus status) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE TripDriver td "
                            + "SET td.status = :status, td.respondedAt = CURRENT_TIMESTAMP "
                            + "WHERE td.trip.id = :tripId "
                            + "AND td.status = :assignedStatus "
                            + "AND td.driver.id = :driverId")
                    .setParameter("status", status)
                    .setParameter("assignedStatus", TripDriverStatus.ASSIGNED)
                    .setParameter("tripId", tripId)
                    .setParameter("driverId", driverId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }

    public void updateAssignedDriversStatus(Integer tripId, TripDriverStatus status) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE TripDriver td "
                            + "SET td.status = :status, td.respondedAt = CURRENT_TIMESTAMP "
                            + "WHERE td.trip.id = :tripId "
                            + "AND td.status = :assignedStatus")
                    .setParameter("status", status)
                    .setParameter("assignedStatus", TripDriverStatus.ASSIGNED)
                    .setParameter("tripId", tripId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }
}
