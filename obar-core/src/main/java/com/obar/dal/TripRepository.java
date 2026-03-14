package com.obar.dal;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import org.hibernate.Session;

import java.util.List;

public class TripRepository extends BaseRepository<Trip, Integer> {

    public TripRepository() {
        super(Trip.class);
    }

    public List<Trip> findByClientId(Integer clientId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Trip t WHERE t.client.id = :clientId", Trip.class)
                    .setParameter("clientId", clientId)
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
}