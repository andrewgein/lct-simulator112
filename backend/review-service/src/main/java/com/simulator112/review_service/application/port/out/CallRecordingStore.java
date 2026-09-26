package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.CallRecording;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallRecordingStore {
    List<CallRecording> findByContextId(UUID contextId);
    Optional<byte[]> findContent(UUID contextId, String callId, String fileName);
}
