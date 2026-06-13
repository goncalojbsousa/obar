package com.obar.dal;

import com.obar.model.TaxRate;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class TaxRateRepository extends BaseRepository<TaxRate, Integer> {

    public TaxRateRepository() {
        super(TaxRate.class);
    }

    public List<TaxRate> findAllOrderedForAdmin() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxRate t ORDER BY t.active DESC, t.rate DESC, t.name ASC",
                    TaxRate.class)
                    .list();
        }
    }

    public List<TaxRate> findActiveOrderedByRateDesc() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxRate t WHERE t.active = true ORDER BY t.rate DESC, t.name ASC",
                    TaxRate.class)
                    .list();
        }
    }

    public Optional<TaxRate> findActiveByRate(BigDecimal rate) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM TaxRate t WHERE t.active = true AND t.rate = :rate",
                    TaxRate.class)
                    .setParameter("rate", rate)
                    .uniqueResultOptional();
        }
    }
}
