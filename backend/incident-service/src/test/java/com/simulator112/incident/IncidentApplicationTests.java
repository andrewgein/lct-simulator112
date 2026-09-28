package com.simulator112.incident;

import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.domain.common.*;
import com.simulator112.incident.domain.dds.*;
import com.simulator112.incident.domain.system112.System112Criteria;
import com.simulator112.incident.domain.system112.System112Incident;
import com.simulator112.incident.domain.system112.System112Stage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "grpc.server.port=0")
class IncidentApplicationTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private com.simulator112.incident.adapter.in.grpc.IncidentGrpcMapper grpcMapper;
    @Test
    void createsNormalizedIncidentSchema() {
        assertThat(tableCount("INCIDENTS")).isEqualTo(1);
        assertThat(tableCount("INCIDENT_STAGES")).isEqualTo(1);
        assertThat(tableCount("SYSTEM112_STAGE_DETAILS")).isEqualTo(1);
        assertThat(tableCount("DDS_STAGE_DETAILS")).isEqualTo(1);
        assertThat(tableCount("CALL_SCENARIOS")).isEqualTo(1);
        assertThat(tableCount("LEVELS")).isZero();
        assertThat(tableCount("LEVEL_INCIDENTS")).isZero();
        assertThat(tableCount("DIALUPS")).isZero();
        assertThat(tableCount("CLASSIFIER_ENTRIES")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'INCIDENTS' AND COLUMN_NAME = 'DDS_INITIAL_STAGE_ID'", Integer.class)).isZero();
    }

    @Test
    void persistsStagesWithCalls() {
        var call = new CallScenario(null, 0, CallDirection.INBOUND, CounterpartyType.CALLER,
                new Person("Иван", "Иванов", null, 35, "+70000000000", null, null, null, null),
                Gender.MAN, List.of("Виден дым"), List.of("Есть пострадавший"), "caller", "WORRIED");
        var stage = new System112Stage(null, "Первичный вызов", 0,
                List.of("101", "102"), 1, "Описание", List.of(call));
        var incident = new System112Incident(null, "Пожар",
                new Address("Москва", "Тверская", "1", null, null, 1), Difficulty.EASY,
                List.of(stage), new System112Criteria(List.of(
                        new DialogueCriterion(null, "Уточнение адреса",
                                "Оператор уточнил адрес происшествия", 10))));

        var saved = incidentRepository.save(incident);
        var loaded = incidentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.stages()).hasSize(1);
        assertThat(loaded.stages()).hasOnlyElementsOfType(System112Stage.class);
        assertThat(((System112Stage) loaded.stages().getFirst()).classifierCodes()).containsExactly("101", "102");
        assertThat(loaded.stages().getFirst().calls()).hasSize(1);
        assertThat(loaded.stages().getFirst().calls().getFirst().direction()).isEqualTo(CallDirection.INBOUND);
        assertThat(((System112Incident) loaded).criteria().dialogueCriteria()).singleElement()
                .satisfies(criterion -> {
                    assertThat(criterion.name()).isEqualTo("Уточнение адреса");
                    assertThat(criterion.weight()).isEqualTo(10);
                });
        assertThat(detailCount("SYSTEM112_STAGE_DETAILS", saved.id())).isEqualTo(1);
        assertThat(detailCount("DDS_STAGE_DETAILS", saved.id())).isZero();
    }

    @Test
    void updatesSystem112StageDetailsWithSharedPrimaryKey() {
        UUID stageId = UUID.randomUUID();
        var original = new System112Incident(null, "Пожар",
                new Address("Москва", "Тверская", "1", null, null, 1), Difficulty.EASY,
                List.of(new System112Stage(stageId, "Первичный вызов", 0,
                        List.of("101"), 0, "Исходное описание", List.of())),
                new System112Criteria(List.of()));
        var saved = incidentRepository.save(original);
        var updated = new System112Incident(saved.id(), "Пожар",
                saved.address(), saved.difficulty(),
                List.of(new System112Stage(stageId, "Первичный вызов", 0,
                        List.of("101", "102"), 2, "Новое описание", List.of())),
                new System112Criteria(List.of()));

        incidentRepository.save(updated);
        var loaded = (System112Incident) incidentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.stages()).singleElement().satisfies(stage -> {
            assertThat(stage.description()).isEqualTo("Новое описание");
            assertThat(stage.victimCount()).isEqualTo(2);
            assertThat(stage.classifierCodes()).containsExactly("101", "102");
        });
        assertThat(detailCount("SYSTEM112_STAGE_DETAILS", saved.id())).isEqualTo(1);
    }

    @Test
    void persistsTimedDdsStagesAndStatusCall() {
        var brigade = new Person("Бригада 12", null, null, null, null, null, null, null, null);
        var outgoing = new CallScenario(null, 0, CallDirection.OUTBOUND, CounterpartyType.BRIGADE,
                brigade, null, List.of("Передана карточка"), List.of(), "dispatch", "CALM");
        UUID firstStageId = UUID.randomUUID();
        UUID secondStageId = UUID.randomUUID();
        UUID lastStageId = UUID.randomUUID();
        var initialStage = new DdsStage(firstStageId, "Уточнение статуса",
                "Позвонить бригаде", DdsStageType.CALL_BRIGADE_FOR_STATUS, 60, List.of(outgoing), "Бригада на месте", com.simulator112.incident.domain.common.IncidentStatus.ARRIVED);
        var successStage = new DdsStage(secondStageId, "Ожидание статуса",
                "Ожидать обновления", DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, 180, List.of());
        var failureStage = new DdsStage(lastStageId, "Завершение",
                "Завершить реагирование", DdsStageType.COMPLETE_INCIDENT, 30, List.of(), null,
                com.simulator112.incident.domain.common.IncidentStatus.VERIFIED);
        var incident = new DdsIncident(null, "Пожар", new Address("Москва", "Тверская", "1", null, null, 1),
                Difficulty.NORMAL, List.of(initialStage, successStage, failureStage),
                new PreparedCardTemplate(List.of("101", "102"), null, 0, java.util.Map.of()),
                new InitialAssignment("CUSTOM_DISPATCH", "101", "Направить ближайшую бригаду"));

        var saved = incidentRepository.save(incident);
        var loaded = (DdsIncident) incidentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.initialAssignment().emergencyService()).isEqualTo("CUSTOM_DISPATCH");
        assertThat(grpcMapper.toProto(loaded).getInitialAssignment().getEmergencyServiceCode())
                .isEqualTo("CUSTOM_DISPATCH");
        assertThat(loaded.stages()).hasOnlyElementsOfType(DdsStage.class);
        assertThat(loaded.stages().getFirst().calls().getFirst().direction()).isEqualTo(CallDirection.OUTBOUND);
        assertThat(loaded.stages().getFirst().expectedComment()).isEqualTo("Бригада на месте");
        assertThat(loaded.stages().getFirst().actualStatus()).isEqualTo(com.simulator112.incident.domain.common.IncidentStatus.ARRIVED);
        assertThat(grpcMapper.toProto(loaded).getStages(0).getDds().getExpectedComment()).isEqualTo("Бригада на месте");
        assertThat(grpcMapper.toProto(loaded).getStages(0).getDds().getActualStatus()).isEqualTo(com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_ARRIVED);
        assertThat(loaded.stages().getLast().actualStatus()).isEqualTo(com.simulator112.incident.domain.common.IncidentStatus.VERIFIED);
        assertThat(grpcMapper.toProto(loaded).getStages(2).getDds().getActualStatus())
                .isEqualTo(com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_VERIFIED);
        assertThat(loaded.stages().getFirst().type()).isEqualTo(DdsStageType.CALL_BRIGADE_FOR_STATUS);
        assertThat(loaded.stages().getFirst().timeLimitSeconds()).isEqualTo(60);
        assertThat(detailCount("DDS_STAGE_DETAILS", saved.id())).isEqualTo(3);
        assertThat(loaded.stages()).extracting(DdsStage::id)
                .containsExactly(firstStageId, secondStageId, lastStageId);
    }

    private Integer tableCount(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                Integer.class,
                tableName);
    }

    private Integer detailCount(String tableName, UUID incidentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " d JOIN incident_stages s ON s.id = d.stage_id "
                        + "WHERE s.incident_id = ?",
                Integer.class,
                incidentId);
    }
}
