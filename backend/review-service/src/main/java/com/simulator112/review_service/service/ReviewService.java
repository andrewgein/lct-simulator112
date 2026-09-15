package com.simulator112.review_service.service;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.review_service.dto.response.AllUserReviewResponse;
import com.simulator112.review_service.dto.response.ReviewResponse;
import com.simulator112.review_service.mapper.ReviewMapper;
import com.simulator112.review_service.model.entity.CriterionResult;
import com.simulator112.review_service.model.entity.Review;
import com.simulator112.review_service.model.enums.ReviewStatus;
import com.simulator112.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    private final Rubric rubric;

    @Transactional
    public ReviewResponse getReviewById(UUID uuid) {
        Review review = find(uuid);
        if (review.getStatus() == ReviewStatus.IN_REVIEW) {
            review.setStatus(ReviewStatus.DONE);
            reviewRepository.save(review);
        }

        return ReviewMapper.toResponse(review);
    }

    @Transactional
    public AllUserReviewResponse getReviewByUser(UUID userId) {
        List<Review> reviews = reviewRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return ReviewMapper.toResponse(reviews);
    }

    @Transactional
    public Review review(FullContext context) {
        List<CriterionResult> result = rubric.evaluate(context);
        Review review = new Review();
        review.setContextId(parseUuid(context.getUuid()));
        if (!context.getUserId().isBlank()) {
            review.setUserId(parseUuid(context.getUserId()));
        }
        if (context.hasLevelContext()) {
            review.setLevelId(context.getLevelContext().getId());
        }
        review.setStatus(ReviewStatus.IN_REVIEW);
        review.setResult(result);
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    private Review find(UUID id) {
        log.debug("Попытка поиска review {} ", id);
        return reviewRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Review не найдено"));
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Невалидный UUID: " + raw);
        }
    }
}
