package com.simulator112.contextmanager.dto.response;

import java.util.UUID;

public record SolutionContextSaveResponse(
        UUID revisionId,
        UUID cardId,
        long version,
        UUID duplicateCardId
) {
}
