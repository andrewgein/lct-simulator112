package com.simulator112.contextmanager.application.model.system112;

import com.simulator112.contextmanager.application.model.system112.PersonInfoRequest;
import com.simulator112.contextmanager.domain.system112.SolutionContextStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SolutionContextView(
        UUID revisionId,
        UUID cardId,
        UUID previousRevisionId,
        long version,
        SolutionContextStatus status,
        UUID callId,
        UUID parentCardId,
        UUID duplicateOfCardId,
        PersonInfoRequest applicant,
        PersonInfoRequest victim,
        Map<String, String> additionalInfo,
        String incidentType,
        Instant createdAt
) {
}
