package com.simulator112.contextmanager.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.CallDirection;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.CounterpartyType;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.DialogTranscript;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.Phrase;
import com.simulator112.contextmanager.domain.common.ReactionStatus;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.common.SpeakerType;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.dds.DdsStageDetails;
import com.simulator112.contextmanager.domain.dds.DdsStageType;
import com.simulator112.contextmanager.domain.system112.System112StageDetails;
import com.simulator112.shared.dto.Difficulty;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void roundTripsDomainAggregateWithoutLeakingJpaEntities() {
        TrainingContext context = new TrainingContext();
        context.setAssignmentId(UUID.randomUUID());
        context.setLevelTitle("Проверочный уровень");
        context.setThreshold3(40);
        context.setThreshold4(60);
        context.setThreshold5(80);
        context.setUserId(UUID.randomUUID());
        context.setTargetType(IncidentTargetType.SYSTEM_112);
        context.setDifficulty(Difficulty.NORMAL);
        context.setExecutionMode(ExecutionMode.SEQUENTIAL);
        context.setStatus(ContextStatus.CREATED);
        context.setDialogStatus(DialogProgressStatus.IDLE);
        context.setDialog(new DialogTranscript(java.util.List.of(new Phrase(SpeakerType.USER, "Здравствуйте", null))));

        IncidentSnapshot incident = new IncidentSnapshot();
        incident.setSourceId(UUID.randomUUID());
        incident.setPosition(0);
        incident.setTitle("Инцидент");
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        StageSnapshot stage = new StageSnapshot();
        stage.setSourceId(UUID.randomUUID());
        stage.setPosition(0);
        stage.setStatus(StageStatus.PENDING);
        stage.setSystem112(new System112StageDetails(java.util.List.of("101"), 2));
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
        entityManager.flush();
        entityManager.clear();
        TrainingContext restored = store.findById(saved.getId()).orElseThrow();

        assertThat(restored.getThreshold3()).isEqualTo(40);
        assertThat(restored.getThreshold4()).isEqualTo(60);
        assertThat(restored.getThreshold5()).isEqualTo(80);
        assertThat(restored.getIncidents()).hasSize(1);
        assertThat(restored.getIncidents().getFirst().getStages().getFirst().getCalls().getFirst().getSourceId())
                .isEqualTo(call.getSourceId());
        assertThat(restored.getIncidents().getFirst().getStages().getFirst().getSystem112().classifierCodes()).containsExactly("101");
        assertThat(restored.getIncidents().getFirst().getStages().getFirst().getSystem112().victimCount()).isEqualTo(2);
        assertThat(restored.getIncidents().getFirst().getStages().getFirst().getDds()).isNull();
        assertThat(restored.getIncidents().getFirst().getServiceReactions().getFirst().getServiceCode())
                .isEqualTo("MCHS");
        assertThat(restored.getIncidents().getFirst().getServiceReactions().getFirst().currentStatus())
                .isEqualTo(ReactionStatus.ADDED);
        assertThat(restored.getCreatedAt()).isNotNull();
        assertThat(restored.getIncidents().getFirst().getCreatedAt()).isNotNull();
        assertThat(restored.getDialog().createdAt()).isNotNull();

        Instant contextCreatedAt = restored.getCreatedAt();
        Instant incidentCreatedAt = restored.getIncidents().getFirst().getCreatedAt();
        Instant dialogCreatedAt = restored.getDialog().createdAt();
        store.save(restored);
        entityManager.flush();
        entityManager.clear();
        TrainingContext savedAgain = store.findById(saved.getId()).orElseThrow();
        assertThat(savedAgain.getIncidents().getFirst().getServiceReactions()).hasSize(1);
        assertThat(savedAgain.getCreatedAt()).isEqualTo(contextCreatedAt);
        assertThat(savedAgain.getIncidents().getFirst().getCreatedAt()).isEqualTo(incidentCreatedAt);
        assertThat(savedAgain.getDialog().createdAt()).isEqualTo(dialogCreatedAt);
    }

    @Test
    void roundTripsDdsStageDetailsAndComment() {
        TrainingContext context = new TrainingContext();
        context.setAssignmentId(UUID.randomUUID());
        context.setUserId(UUID.randomUUID());
        context.setTargetType(IncidentTargetType.DDS);
        context.setExecutionMode(ExecutionMode.PARALLEL);
        context.setStatus(ContextStatus.CREATED);
        context.setDialogStatus(DialogProgressStatus.IDLE);
        IncidentSnapshot incident = new IncidentSnapshot();
        incident.setSourceId(UUID.randomUUID());
        incident.setPosition(0);
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        incident.setPreparedCardAssignedServices(java.util.List.of("MCHS", "POLICE"));
        StageSnapshot stage = new StageSnapshot();
        stage.setSourceId(UUID.randomUUID());
        stage.setPosition(0);
        stage.setStatus(StageStatus.ACTIVE);
        stage.setDds(new DdsStageDetails(DdsStageType.CALL_BRIGADE_FOR_STATUS, 90,
                "Бригада прибыла", "Бригада на месте"));
        stage.getDds().setCompletionTriggers(java.util.List.of(
                com.simulator112.contextmanager.domain.dds.DdsCompletionTrigger.TIME,
                com.simulator112.contextmanager.domain.dds.DdsCompletionTrigger.CALLS));
        var serviceCall = new com.simulator112.contextmanager.domain.common.CallSnapshot();
        serviceCall.setSourceId(UUID.randomUUID());
        serviceCall.setPosition(0);
        serviceCall.setDirection(com.simulator112.contextmanager.domain.common.CallDirection.INBOUND);
        serviceCall.setCounterparty(com.simulator112.contextmanager.domain.common.CounterpartyType.SERVICE);
        serviceCall.setServiceCode("MCHS");
        serviceCall.setStatus(com.simulator112.contextmanager.domain.common.CallStatus.PENDING);
        stage.getCalls().add(serviceCall);
        incident.getStages().add(stage);
        context.getIncidents().add(incident);

        TrainingContext saved = store.save(context);
        TrainingContext restored = store.findById(saved.getId()).orElseThrow();
        StageSnapshot actual = restored.getIncidents().getFirst().getStages().getFirst();
        assertThat(restored.getIncidents().getFirst().getPreparedCardAssignedServices()).containsExactly("MCHS", "POLICE");
        assertThat(actual.getDds().getType()).isEqualTo(DdsStageType.CALL_BRIGADE_FOR_STATUS);
        assertThat(actual.getDds().getTimeLimitSeconds()).isEqualTo(90);
        assertThat(actual.getDds().getExpectedComment()).isEqualTo("Бригада прибыла");
        assertThat(actual.getDds().getComment()).isEqualTo("Бригада на месте");
        assertThat(actual.getDds().getCompletionTriggers()).containsExactlyInAnyOrder(
                com.simulator112.contextmanager.domain.dds.DdsCompletionTrigger.TIME,
                com.simulator112.contextmanager.domain.dds.DdsCompletionTrigger.CALLS);
        assertThat(actual.getCalls().getFirst().getServiceCode()).isEqualTo("MCHS");
        assertThat(actual.getSystem112()).isNull();

        actual.getDds().setComment("Требуется подкрепление");
        store.save(restored);
        assertThat(store.findById(saved.getId()).orElseThrow().getIncidents().getFirst().getStages().getFirst()
                .getDds().getComment()).isEqualTo("Требуется подкрепление");
    }
}
