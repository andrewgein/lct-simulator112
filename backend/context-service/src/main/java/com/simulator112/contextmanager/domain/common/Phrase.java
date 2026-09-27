package com.simulator112.contextmanager.domain.common;

import java.util.UUID;

public record Phrase(SpeakerType speaker, String text, UUID callId) {
}
