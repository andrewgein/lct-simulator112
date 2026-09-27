package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.Review;

import java.util.Map;
import java.util.UUID;

public interface UpdateCriterionScoresUseCase {
    Review updateScores(UUID contextId, UUID expertId, Map<UUID, Integer> corrections);
}
