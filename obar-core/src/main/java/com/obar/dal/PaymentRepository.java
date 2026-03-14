package com.obar.dal;

import com.obar.model.Payment;
import org.hibernate.Session;

import java.util.List;

public class PaymentRepository extends BaseRepository<Payment, Integer> {

    public PaymentRepository() {
        super(Payment.class);
    }

    public List<Payment> findByTripId(Integer tripId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Payment p WHERE p.trip.id = :tripId", Payment.class)
                    .setParameter("tripId", tripId)
                    .list();
        }
    }
}