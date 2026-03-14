package com.obar.bll;

import com.obar.dal.ReviewRepository;
import com.obar.dal.TripRepository;
import com.obar.dal.UserRepository;
import com.obar.model.Review;
import com.obar.model.enums.TripStatus;

import java.util.List;

public class ReviewService {

    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final TripRepository tripRepository = new TripRepository();
    private final UserRepository userRepository = new UserRepository();

    public Review addReview(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating tem de ser entre 1 e 5.");
        }

        // Só se pode avaliar viagens concluídas
        tripRepository.findById(review.getTrip().getId()).ifPresent(t -> {
            if (t.getStatus() != TripStatus.COMPLETED) {
                throw new IllegalStateException("Só é possível avaliar viagens concluídas.");
            }
        });

        Review saved = reviewRepository.save(review);

        // Actualiza a média do avaliado
        updateAverageRating(review.getReviewed().getId());

        return saved;
    }

    public List<Review> findByReviewed(Integer reviewedId) {
        return reviewRepository.findByReviewedId(reviewedId);
    }

    private void updateAverageRating(Integer userId) {
        List<Review> reviews = reviewRepository.findByReviewedId(userId);
        if (reviews.isEmpty()) return;

        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);

        userRepository.findById(userId).ifPresent(u -> {
            u.setAverageRating((float) avg);
            userRepository.update(u);
        });
    }
}