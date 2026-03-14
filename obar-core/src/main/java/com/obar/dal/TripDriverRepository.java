package com.obar.dal;

import com.obar.model.TripDriver;
import org.hibernate.Session;

import java.util.List;

public class TripDriverRepository extends BaseRepository<TripDriver, Integer> {

    public TripDriverRepository() {
        super(TripDriver.class);
    }

    public List<TripDriver> findByTripId(Integer tripId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM TripDriver td WHERE td.trip.id = :tripId", TripDriver.class)
                    .setParameter("tripId", tripId)
                    .list();
        }
    }
}