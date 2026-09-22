package com.simulator112.contextmanager.application.model.system112;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SolutionContextView(
        UUID revisionId,
        UUID cardId,
        UUID previousRevisionId,
        long version,
        UUID callId,
        UUID mainCardId,
        PersonInfoRequest applicant,
        int victimCount,
        Map<String, String> additionalInfo,
        List<String> incidentTypes,
        Instant createdAt
) {
}
