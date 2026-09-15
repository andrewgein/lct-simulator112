package com.simulator112.review_service.repository;

import java.util.List;
import java.util.UUID;

import com.simulator112.review_service.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
