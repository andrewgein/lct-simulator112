package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.IncidentStatus;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdsStageStatusTests {
    @Test
    void acceptsAnyReactionStatusForStatusTrigger() {
        for (IncidentStatus status : List.of(IncidentStatus.ADDED, IncidentStatus.RECEIVED_BY_SERVICE,
                IncidentStatus.ACCEPTED, IncidentStatus.NOT_ACCEPTED, IncidentStatus.RESPONSE_STARTED,
                IncidentStatus.ARRIVED, IncidentStatus.WORK_IN_PROGRESS, IncidentStatus.WORK_COMPLETED,
                IncidentStatus.WORK_REFUSED)) {
            assertThatCode(() -> stage(status, List.of(DdsCompletionTrigger.STATUS))).doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsSystemStatusEvenWithoutStatusTrigger() {
        assertThatThrownBy(() -> stage(IncidentStatus.COMPLETED, List.of(DdsCompletionTrigger.TIME)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("статусом реагирования");
    }

    private DdsStage stage(IncidentStatus status, List<DdsCompletionTrigger> triggers) {
        return new DdsStage(UUID.randomUUID(), "Этап", null, DdsStageType.COMPLETE_INCIDENT,
                60, List.of(), null, status, triggers);
    }
}
