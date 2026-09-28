package com.simulator112.contextmanager.domain.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record DialogTranscript(List<Phrase> phrases, Instant createdAt, Instant updatedAt) {
    public DialogTranscript(List<Phrase> phrases) {
        this(phrases, null, null);
    }

    public DialogTranscript {
        phrases = new ArrayList<>(phrases);
    }
}
