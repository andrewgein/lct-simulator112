package com.simulator112.contextmanager.dto.response;

import com.simulator112.contextmanager.dto.request.PersonInfoRequest;
import com.simulator112.contextmanager.model.enums.SolutionContextStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SolutionContextView(
        UUID revisionId,
        UUID cardId,
        UUID previousRevisionId,
        long version,
        SolutionContextStatus status,
        UUID dialupId,
        UUID parentCardId,
        UUID duplicateOfCardId,
        PersonInfoRequest applicant,
        PersonInfoRequest victim,
        Map<String, String> additionalInfo,
        String incidentType,
        Instant createdAt
) {
}
