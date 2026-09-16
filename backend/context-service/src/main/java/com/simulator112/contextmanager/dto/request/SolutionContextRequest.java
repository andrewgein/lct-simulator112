package com.simulator112.contextmanager.dto.request;

import com.simulator112.contextmanager.model.enums.SolutionContextOperation;

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
