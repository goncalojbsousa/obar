package com.obar.dal;

import com.obar.model.PaymentMethod;
import org.hibernate.Session;

import java.util.List;

public class PaymentMethodRepository extends BaseRepository<PaymentMethod, Integer> {

    public PaymentMethodRepository() {
        super(PaymentMethod.class);
    }

    public List<PaymentMethod> findByClientId(Integer clientId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM PaymentMethod p WHERE p.client.id = :clientId AND p.active = true", PaymentMethod.class)
                    .setParameter("clientId", clientId)
                    .list();
        }
    }
}