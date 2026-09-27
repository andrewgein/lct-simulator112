package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.PersonalStatistics;

import java.util.UUID;

public interface GetPersonalStatisticsUseCase {
    PersonalStatistics getForUser(UUID userId);
}
