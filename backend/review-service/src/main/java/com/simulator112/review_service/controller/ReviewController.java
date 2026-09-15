package com.simulator112.review_service.controller;

import com.simulator112.review_service.dto.response.AllUserReviewResponse;
import com.simulator112.review_service.dto.response.ReviewResponse;
import com.simulator112.review_service.model.enums.ReviewStatus;
import com.simulator112.review_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<AllUserReviewResponse> getUserReviews(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(reviewService.getReviewByUser(userId));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable UUID uuid){
        // TOOD: kolhoz
        try {
            ReviewResponse review = reviewService.getReviewById(uuid);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.info("Проверка {} еще не закончена", uuid);
            return ResponseEntity.ok(new ReviewResponse(uuid, null, null, List.of(), ReviewStatus.FILLED.toString()));
        }
    }
}
