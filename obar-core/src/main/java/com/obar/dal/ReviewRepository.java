package com.obar.dal;

import com.obar.model.Review;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public class ReviewRepository extends BaseRepository<Review, Integer> {

    public ReviewRepository() {
        super(Review.class);
    }

    public List<Review> findByReviewedId(Integer reviewedId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Review r WHERE r.reviewed.id = :reviewedId", Review.class)
                    .setParameter("reviewedId", reviewedId)
                    .list();
        }
    }

    public boolean existsByTripIdAndReviewerType(Integer tripId, String reviewerType) {
        try (Session session = getSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(r.id) FROM Review r "
                                    + "WHERE r.trip.id = :tripId "
                                    + "AND r.reviewerType = :reviewerType",
                            Long.class)
                    .setParameter("tripId", tripId)
                    .setParameter("reviewerType", reviewerType)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public Optional<Review> findByTripIdAndReviewerType(Integer tripId, String reviewerType) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "SELECT r FROM Review r "
                                    + "WHERE r.trip.id = :tripId "
                                    + "AND r.reviewerType = :reviewerType",
                            Review.class)
                    .setParameter("tripId", tripId)
                    .setParameter("reviewerType", reviewerType)
                    .uniqueResultOptional();
        }
    }
}
