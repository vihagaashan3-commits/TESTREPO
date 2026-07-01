package com.roadrescue.service;

import com.roadrescue.entity.*;
import com.roadrescue.exception.ResourceNotFoundException;
import com.roadrescue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final GarageService garageService;

    @Transactional
    public Review addReview(Long garageId, String userEmail, Integer rating, String comment) {
        User user = userService.findByEmail(userEmail);
        Garage garage = garageService.findById(garageId);

        if (reviewRepository.existsByUserIdAndGarageId(user.getId(), garageId)) {
            throw new IllegalArgumentException("You have already reviewed this garage.");
        }

        Review review = Review.builder()
                .user(user).garage(garage)
                .rating(rating).comment(comment)
                .build();
        return reviewRepository.save(review);
    }

    public Page<Review> getGarageReviews(Long garageId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByGarageIdAndDeletedFalse(garageId, pageable);
    }

    /**
     * Fetches all non-deleted reviews for a garage, newest first.
     * Used on the garage detail page so the list is loaded eagerly
     * inside this transactional method instead of lazily off the entity.
     */
    public List<Review> getAllGarageReviews(Long garageId) {
        Pageable pageable = PageRequest.of(0, 100, Sort.by("createdAt").descending());
        return reviewRepository.findByGarageIdAndDeletedFalse(garageId, pageable).getContent();
    }

    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    @Transactional
    public void softDelete(Long id) {
        Review review = findById(id);
        review.setDeleted(true);
        reviewRepository.save(review);
    }
}