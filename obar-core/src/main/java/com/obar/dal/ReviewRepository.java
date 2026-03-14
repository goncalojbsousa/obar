package com.obar.dal;

import com.obar.model.Review;
import org.hibernate.Session;

import java.util.List;

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
}