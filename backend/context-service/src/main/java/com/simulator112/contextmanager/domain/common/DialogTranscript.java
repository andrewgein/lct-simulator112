package com.simulator112.contextmanager.domain.common;

import java.util.ArrayList;
import java.util.List;

public record DialogTranscript(List<Phrase> phrases) {
    public DialogTranscript {
        phrases = new ArrayList<>(phrases);
    }
}
