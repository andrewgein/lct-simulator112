package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.CriterionResultResponse;
import com.simulator112.review_service.adapter.in.rest.dto.DispatcherCardResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ErrorStatisticResponse;
import com.simulator112.review_service.adapter.in.rest.dto.IncidentSummaryResponse;
import com.simulator112.review_service.adapter.in.rest.dto.PersonalStatisticsResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.domain.model.DispatcherCardSummary;
import com.simulator112.review_service.domain.model.PersonalStatistics;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewSubmission;

final class ReviewRestMapper {
    private ReviewRestMapper() {
    }

    static PersonalStatisticsResponse toResponse(PersonalStatistics statistics) {
        return new PersonalStatisticsResponse(statistics.errorStatistics().stream()
                .map(value -> new ErrorStatisticResponse(value.criterionName(), value.attempts(), value.failedCount(),
                        value.scoreEarned(), value.scoreMax(), value.errorRate()))
                .toList(), statistics.recommendations());
    }

    static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(review.contextId(), review.userId(), review.assignmentId(), review.createdAt(),
                review.results().stream().map(value -> new CriterionResultResponse(value.id(), value.incidentId(),
                                value.incidentOrder(), value.criterionName(), value.score(), value.maxScore(), value.feedback()))
                        .toList(),
                review.automaticScore(), review.finalScore(), review.maxScore(), review.durationSeconds(),
                review.timeLimitSeconds(), review.overtimeSeconds(), review.expertId(), review.expertComment(),
                review.confirmedAt(), review.status().name(), review.grade(), review.passed(),
                review.incidents().stream().map(value -> new IncidentSummaryResponse(value.id(), value.order(),
                        value.title(), value.victimCount(), value.classifierCodes())).toList(),
                review.cards().stream().map(ReviewRestMapper::toResponse).toList());
    }

    private static DispatcherCardResponse toResponse(DispatcherCardSummary card) {
        return new DispatcherCardResponse(card.cardId(), card.callId(), card.mainCardId(), card.incidentId(), toResponse(card.applicant()),
                card.victimCount(), card.incidentTypes(), card.services(), card.additionalInfo());
    }

    private static DispatcherCardResponse.PersonResponse toResponse(ReviewSubmission.Person person) {
        if (person == null) return null;
        return new DispatcherCardResponse.PersonResponse(person.firstName(), person.lastName(), person.middleName(),
                person.phone(), person.contactPhone(), person.onScenePhone(), person.address(), person.additionalInfo());
    }
}
