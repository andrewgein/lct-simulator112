package com.simulator112.contextmanager.adapter.grpc.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.simulator112.incident.grpc.contract.CallDirection;
import com.simulator112.incident.grpc.contract.CallScenario;
import com.simulator112.incident.grpc.contract.CounterpartyType;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.IncidentStage;
import com.simulator112.incident.grpc.contract.Person;
import com.simulator112.incident.grpc.contract.System112StageDetails;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentContextMapperTests {
    @Test
    void preservesCallParticipantWithoutUsingIncidentAddress() {
        Person caller = Person.newBuilder()
                .setFirstName("Анна")
                .setAddress("Адрес заявителя")
                .build();
        UUID callId = UUID.randomUUID();
        IncidentContext source = IncidentContext.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTitle("Пожар")
                .setTargetType(com.simulator112.incident.grpc.contract.IncidentTargetType.INCIDENT_TARGET_TYPE_SYSTEM_112)
                .setDifficulty(com.simulator112.incident.grpc.contract.Difficulty.DIFFICULTY_EASY)
                .addStages(IncidentStage.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setSystem112(System112StageDetails.newBuilder()
                                .setPosition(0)
                                .addClassifierCodes("101")
                                .addClassifierCodes("102"))
                        .addCalls(CallScenario.newBuilder()
                                .setId(callId.toString())
                                .setPosition(0)
                                .setDirection(CallDirection.CALL_DIRECTION_INBOUND)
                                .setCounterparty(CounterpartyType.COUNTERPARTY_TYPE_CALLER)
                                .setPerson(caller)))
                .build();

        var stored = IncidentContextMapper.toDomain(source, 0);
        CallScenario result = IncidentContextMapper.toProto(
                stored.getStages().getFirst().getCalls().getFirst());

        assertThat(stored.getStages().getFirst().getClassifierCodes()).containsExactly("101", "102");
        assertThat(IncidentContextMapper.toProto(stored).getStages(0).getSystem112().getClassifierCodesList())
                .containsExactly("101", "102");
        assertThat(result.getId()).isEqualTo(callId.toString());
        assertThat(result.getPerson().getFirstName()).isEqualTo("Анна");
        assertThat(result.getPerson().getAddress()).isEqualTo("Адрес заявителя");
        assertThat(result.getDirection()).isEqualTo(CallDirection.CALL_DIRECTION_INBOUND);
    }
}
