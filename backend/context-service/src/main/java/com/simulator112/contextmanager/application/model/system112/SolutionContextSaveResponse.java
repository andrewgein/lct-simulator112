package com.simulator112.contextmanager.application.model.system112;

import java.util.UUID;

public record SolutionContextSaveResponse(
        UUID revisionId,
        UUID cardId,
        long version,
        UUID duplicateCardId
) {
}
