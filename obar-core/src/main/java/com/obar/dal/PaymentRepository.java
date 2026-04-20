package com.obar.dal;

import com.obar.model.Payment;
import com.obar.model.enums.PaymentStatus;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    public List<Payment> findAllForAdmin(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        try (Session session = getSession()) {
            StringBuilder hql = new StringBuilder("SELECT p FROM Payment p ")
                    .append("LEFT JOIN FETCH p.trip t ")
                    .append("LEFT JOIN FETCH t.client ")
                    .append("LEFT JOIN FETCH t.driver ")
                    .append("LEFT JOIN FETCH p.paymentMethod ")
                    .append("LEFT JOIN FETCH p.currency ")
                    .append("LEFT JOIN FETCH p.taxRate ");

            appendPeriodFilter(hql, startInclusive, endExclusive, false);
            hql.append(" ORDER BY p.paymentDate DESC");

            var query = session.createQuery(hql.toString(), Payment.class);
            bindPeriodParameters(query, startInclusive, endExclusive);
            return query.list();
        }
    }

    public BigDecimal sumAmountByStatus(PaymentStatus status, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        try (Session session = getSession()) {
            StringBuilder hql = new StringBuilder(
                    "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status");
            appendPeriodFilter(hql, startInclusive, endExclusive, true);

            var query = session.createQuery(hql.toString(), BigDecimal.class)
                    .setParameter("status", status);
            bindPeriodParameters(query, startInclusive, endExclusive);
            return query.uniqueResult();
        }
    }

    public List<Object[]> countByStatus(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        try (Session session = getSession()) {
            StringBuilder hql = new StringBuilder(
                    "SELECT p.status, COUNT(p.id) FROM Payment p");
            appendPeriodFilter(hql, startInclusive, endExclusive, false);
            hql.append(" GROUP BY p.status");

            var query = session.createQuery(hql.toString(), Object[].class);
            bindPeriodParameters(query, startInclusive, endExclusive);
            return query.list();
        }
    }

    private void appendPeriodFilter(StringBuilder hql, LocalDateTime startInclusive, LocalDateTime endExclusive,
            boolean hasWhereClause) {
        boolean needsFilter = startInclusive != null || endExclusive != null;
        if (!needsFilter) {
            return;
        }

        hql.append(hasWhereClause ? " AND" : " WHERE");
        boolean hasStart = startInclusive != null;
        if (hasStart) {
            hql.append(" p.paymentDate >= :startInclusive");
        }
        if (hasStart && endExclusive != null) {
            hql.append(" AND");
        }
        if (endExclusive != null) {
            hql.append(" p.paymentDate < :endExclusive");
        }
    }

    private void bindPeriodParameters(org.hibernate.query.Query<?> query, LocalDateTime startInclusive,
            LocalDateTime endExclusive) {
        if (startInclusive != null) {
            query.setParameter("startInclusive", startInclusive);
        }
        if (endExclusive != null) {
            query.setParameter("endExclusive", endExclusive);
        }
    }
}