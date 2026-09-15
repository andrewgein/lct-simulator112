package com.simulator112.review_service.mapper;

import com.simulator112.review_service.model.entity.CriterionResult;
import com.simulator112.review_service.model.entity.Review;
import com.simulator112.review_service.model.enums.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewMapperTests {
    @Test
    void exposesIncidentIdentifierAndOrderInCriterionContract() {
        CriterionResult result = new CriterionResult();
        result.setIncidentId("incident-id");
        result.setIncidentOrder(2);
        result.setCriterionName("Поля");
        result.setScore(5);
        result.setMaxScore(10);
        result.setFeedback("Ошибка");

        var dto = ReviewMapper.toDto(result);

        assertEquals("incident-id", dto.incidentId());
        assertEquals(2, dto.incidentOrder());
        assertEquals("Поля", dto.criterionName());
    }

    @Test
    void exposesLevelIdentifierInReviewContract() {
        Review review = new Review();
        review.setContextId(UUID.randomUUID());
        review.setLevelId(UUID.randomUUID().toString());
        review.setCreatedAt(Instant.now());
        review.setResult(List.of());
        review.setStatus(ReviewStatus.DONE);

        var response = ReviewMapper.toResponse(review);

        assertEquals(review.getLevelId(), response.levelId());
        assertEquals(review.getCreatedAt(), response.createdAt());
    }
}
