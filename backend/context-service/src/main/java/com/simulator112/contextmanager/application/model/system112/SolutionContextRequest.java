package com.simulator112.contextmanager.application.model.system112;

import com.simulator112.contextmanager.domain.system112.SolutionContextOperation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SolutionContextRequest(
        PersonInfoRequest applicant,
        Integer victimCount,
        Map<String, String> additionalInfo,
        List<String> incidentTypes,
        List<String> services,
        UUID cardId,
        Long expectedVersion,
        SolutionContextOperation operation,
        UUID mainCardId
) {
}
