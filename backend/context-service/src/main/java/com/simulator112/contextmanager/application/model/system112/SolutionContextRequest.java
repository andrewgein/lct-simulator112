package com.simulator112.contextmanager.application.model.system112;

import com.simulator112.contextmanager.domain.system112.SolutionContextOperation;

import java.util.Map;
import java.util.UUID;

public record SolutionContextRequest(
        PersonInfoRequest applicant,
        PersonInfoRequest victim,
        Map<String, String> additionalInfo,
        String incidentType,
        UUID cardId,
        Long expectedVersion,
        SolutionContextOperation operation,
        UUID parentCardId
) {
}
