package com.simulator112.contextmanager.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.CallDirection;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.CounterpartyType;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.ReactionStatus;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.shared.dto.Difficulty;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ContextPersistenceAdapterTests {
    @Autowired
    private ContextStore store;

    @Test
    void roundTripsDomainAggregateWithoutLeakingJpaEntities() {
        TrainingContext context = new TrainingContext();
        context.setAssignmentId(UUID.randomUUID());
        context.setLevelTitle("Проверочный уровень");
        context.setUserId(UUID.randomUUID());
        context.setTargetType(IncidentTargetType.SYSTEM_112);
        context.setDifficulty(Difficulty.NORMAL);
        context.setExecutionMode(ExecutionMode.SEQUENTIAL);
        context.setStatus(ContextStatus.CREATED);
        context.setDialogStatus(DialogProgressStatus.IDLE);

        IncidentSnapshot incident = new IncidentSnapshot();
        incident.setSourceId(UUID.randomUUID());
        incident.setPosition(0);
        incident.setTitle("Инцидент");
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        StageSnapshot stage = new StageSnapshot();
        stage.setSourceId(UUID.randomUUID());
        stage.setPosition(0);
        stage.setStatus(StageStatus.PENDING);
        CallSnapshot call = new CallSnapshot();
        call.setSourceId(UUID.randomUUID());
        call.setPosition(0);
        call.setQueuePosition(0);
        call.setDirection(CallDirection.INBOUND);
        call.setCounterparty(CounterpartyType.CALLER);
        call.setStatus(CallStatus.PENDING);
        stage.getCalls().add(call);
        incident.getStages().add(stage);
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ADDED, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        context.getIncidents().add(incident);

        TrainingContext saved = store.save(context);
        TrainingContext restored = store.findById(saved.getId()).orElseThrow();

        assertThat(restored.getIncidents()).hasSize(1);
        assertThat(restored.getIncidents().getFirst().getStages().getFirst().getCalls().getFirst().getSourceId())
                .isEqualTo(call.getSourceId());
        assertThat(restored.getIncidents().getFirst().getServiceReactions().getFirst().getServiceCode())
                .isEqualTo("MCHS");
        assertThat(restored.getIncidents().getFirst().getServiceReactions().getFirst().currentStatus())
                .isEqualTo(ReactionStatus.ADDED);

        store.save(restored);
        assertThat(store.findById(saved.getId()).orElseThrow().getIncidents().getFirst().getServiceReactions())
                .hasSize(1);
    }
}
