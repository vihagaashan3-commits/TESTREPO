package com.roadrescue.repository;

import com.roadrescue.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByGarageIdAndDeletedFalse(Long garageId, Pageable pageable);
    Page<Review> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    boolean existsByUserIdAndGarageId(Long userId, Long garageId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.garage.id = :garageId AND r.deleted = false")
    Double getAverageRatingByGarageId(@Param("garageId") Long garageId);
}
