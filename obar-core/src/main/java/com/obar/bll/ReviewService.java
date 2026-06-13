package com.obar.bll;

import com.obar.dal.ReviewRepository;
import com.obar.dal.TripRepository;
import com.obar.dal.UserRepository;
import com.obar.model.Review;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.UserType;

import java.util.List;
import java.util.Optional;

public class ReviewService {

    private static final String CLIENT_REVIEWER_TYPE = "CLIENT";

    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final TripRepository tripRepository = new TripRepository();
    private final UserRepository userRepository = new UserRepository();

    public Review addReview(Review review) {
        if (review == null || review.getRating() == null
                || review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Escolhe uma nota entre 1 e 5.");
        }
        if (review.getTrip() == null || review.getTrip().getId() == null) {
            throw new IllegalArgumentException("Viagem nao encontrada.");
        }
        if (review.getReviewerType() == null || review.getReviewerType().isBlank()) {
            throw new IllegalArgumentException("Tipo de avaliador invalido.");
        }

        Trip trip = tripRepository.findById(review.getTrip().getId())
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new IllegalStateException("So e possivel avaliar viagens concluidas.");
        }
        if (reviewRepository.existsByTripIdAndReviewerType(trip.getId(), review.getReviewerType())) {
            throw new IllegalStateException("Esta viagem ja foi avaliada.");
        }

        Review saved = reviewRepository.save(review);
        updateAverageRating(review.getReviewed().getId());
        return saved;
    }

    public List<Review> findByReviewed(Integer reviewedId) {
        return reviewRepository.findByReviewedId(reviewedId);
    }

    public int countByReviewed(Integer reviewedId) {
        return reviewRepository.findByReviewedId(reviewedId).size();
    }

    public Optional<Trip> findPendingClientReview(Integer clientId) {
        return tripRepository.findLatestCompletedWithoutClientReview(clientId);
    }

    public Review addClientReview(Integer tripId, Integer clientId, Integer rating, String comment) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new IllegalStateException("So e possivel avaliar viagens concluidas.");
        }
        if (trip.getClient() == null || !trip.getClient().getId().equals(clientId)) {
            throw new IllegalStateException("Nao podes avaliar esta viagem.");
        }
        if (trip.getDriver() == null || trip.getDriver().getType() != UserType.DRIVER) {
            throw new IllegalStateException("A viagem nao tem um motorista para avaliar.");
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        Review review = new Review();
        review.setTrip(trip);
        review.setReviewer(client);
        review.setReviewed(trip.getDriver());
        review.setRating(rating);
        review.setComment(normalizeComment(comment));
        review.setReviewerType(CLIENT_REVIEWER_TYPE);
        return addReview(review);
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String normalized = comment.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("O comentario nao pode exceder 500 caracteres.");
        }
        return normalized;
    }

    private void updateAverageRating(Integer userId) {
        List<Review> reviews = reviewRepository.findByReviewedId(userId);
        if (reviews.isEmpty()) {
            return;
        }

        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);

        userRepository.findById(userId).ifPresent(user -> {
            user.setAverageRating((float) average);
            userRepository.update(user);
        });
    }
}
