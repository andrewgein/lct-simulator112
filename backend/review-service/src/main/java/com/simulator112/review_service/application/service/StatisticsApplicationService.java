package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.port.in.GetPersonalStatisticsUseCase;
import com.simulator112.review_service.application.port.out.RecommendationPort;
import com.simulator112.review_service.application.port.out.RecommendationStore;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.evaluation.ErrorStatisticsAggregator;
import com.simulator112.review_service.domain.model.CachedRecommendations;
import com.simulator112.review_service.domain.model.ErrorStatistic;
import com.simulator112.review_service.domain.model.PersonalStatistics;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsApplicationService implements GetPersonalStatisticsUseCase {
    private final ReviewStore reviewStore;
    private final RecommendationPort recommendationPort;
    private final RecommendationStore recommendationStore;

    @Override
    @Transactional
    public PersonalStatistics getForUser(UUID userId) {
        List<Review> doneReviews = reviewStore.findByUserId(userId).stream()
                .filter(review -> review.status() == ReviewStatus.DONE)
                .toList();
        List<ErrorStatistic> statistics = ErrorStatisticsAggregator.aggregate(doneReviews);
        return new PersonalStatistics(statistics, recommendationsFor(userId, statistics, doneReviews.size()));
    }

    private List<String> recommendationsFor(UUID userId, List<ErrorStatistic> statistics, int reviewsCount) {
        if (statistics.isEmpty()) return List.of();
        var cached = recommendationStore.findByUserId(userId);
        if (cached.isPresent() && cached.get().reviewsCount() == reviewsCount) {
            return cached.get().recommendations();
        }
        List<String> recommendations = recommendationPort.recommend(statistics);
        recommendationStore.save(new CachedRecommendations(userId, recommendations, reviewsCount, Instant.now()));
        return recommendations;
    }
}
