package com.simulator112.incident;

import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.application.port.out.LevelRepository;
import com.simulator112.incident.domain.common.*;
import com.simulator112.incident.domain.dds.*;
import com.simulator112.incident.domain.system112.System112Criteria;
import com.simulator112.incident.domain.system112.System112Incident;
import com.simulator112.incident.domain.system112.System112Stage;
import com.simulator112.incident.domain.level.ExecutionMode;
import com.simulator112.incident.domain.level.Level;
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
    private LevelRepository levelRepository;

    @Test
    void createsNormalizedIncidentAndLevelSchema() {
        assertThat(tableCount("INCIDENTS")).isEqualTo(1);
        assertThat(tableCount("INCIDENT_STAGES")).isEqualTo(1);
        assertThat(tableCount("SYSTEM112_STAGE_DETAILS")).isEqualTo(1);
        assertThat(tableCount("DDS_STAGE_DETAILS")).isEqualTo(1);
        assertThat(tableCount("CALL_SCENARIOS")).isEqualTo(1);
        assertThat(tableCount("LEVELS")).isEqualTo(1);
        assertThat(tableCount("LEVEL_INCIDENTS")).isEqualTo(1);
        assertThat(tableCount("DIALUPS")).isZero();
        assertThat(tableCount("CLASSIFIER_ENTRIES")).isZero();
    }

    @Test
    void persistsStagesWithCalls() {
        var call = new CallScenario(null, 0, CallDirection.INBOUND, CounterpartyType.CALLER,
                new Person("Иван", "Иванов", null, 35, "+70000000000", null, null, null),
                Gender.MAN, List.of("Виден дым"), List.of("Есть пострадавший"), "caller", "WORRIED");
        var stage = new System112Stage(null, "Первичный вызов", 0,
                List.of("101", "102"), 1, "Описание", List.of(call));
        var incident = new System112Incident(null, "Пожар",
                new Address("Москва", "Тверская", "1", null, null, 1), Difficulty.EASY,
                List.of(stage), new System112Criteria(
                List.of("Адрес?"), List.of("Передать карточку"), List.of()));

        var saved = incidentRepository.save(incident);
        var loaded = incidentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.stages()).hasSize(1);
        assertThat(loaded.stages()).hasOnlyElementsOfType(System112Stage.class);
        assertThat(((System112Stage) loaded.stages().getFirst()).classifierCodes()).containsExactly("101", "102");
        assertThat(loaded.stages().getFirst().calls()).hasSize(1);
        assertThat(loaded.stages().getFirst().calls().getFirst().direction()).isEqualTo(CallDirection.INBOUND);
        assertThat(detailCount("SYSTEM112_STAGE_DETAILS", saved.id())).isEqualTo(1);
        assertThat(detailCount("DDS_STAGE_DETAILS", saved.id())).isZero();
    }

    @Test
    void persistsOrderedLevelIncidents() {
        var call = new CallScenario(null, 0, CallDirection.INBOUND, CounterpartyType.CALLER,
                null, null, List.of(), List.of(), null, null);
        var stage = new System112Stage(null, "Вызов", 0, List.of("101"), 0, null, List.of(call));
        var first = incidentRepository.save(new System112Incident(null, "Первый",
                new Address("Москва", "Тверская", "1", null, null, null), Difficulty.EASY,
                List.of(stage), new System112Criteria(List.of(), List.of(), List.of())));
        var second = incidentRepository.save(new System112Incident(null, "Второй",
                new Address("Москва", "Тверская", "2", null, null, null), Difficulty.EASY,
                List.of(stage), new System112Criteria(List.of(), List.of(), List.of())));

        var saved = levelRepository.save(new Level(null, "Параллельный уровень",
                IncidentTargetType.SYSTEM_112, Difficulty.EASY, ExecutionMode.PARALLEL,
                List.of(first.id(), second.id())));
        var loaded = levelRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.incidentIds()).containsExactly(first.id(), second.id());
    }

    @Test
    void persistsTimedDdsStagesAndStatusCall() {
        var brigade = new Person("Бригада 12", null, null, null, null, null, null, null);
        var outgoing = new CallScenario(null, 0, CallDirection.OUTBOUND, CounterpartyType.BRIGADE,
                brigade, null, List.of("Передана карточка"), List.of(), "dispatch", "CALM");
        UUID initialStageId = UUID.randomUUID();
        UUID successStageId = UUID.randomUUID();
        UUID failureStageId = UUID.randomUUID();
        var initialStage = new DdsStage(initialStageId, "Уточнение статуса",
                "Позвонить бригаде", DdsStageType.CALL_BRIGADE_FOR_STATUS, 60, List.of(outgoing));
        var successStage = new DdsStage(successStageId, "Ожидание статуса",
                "Ожидать обновления", DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, 180, List.of());
        var failureStage = new DdsStage(failureStageId, "Завершение",
                "Завершить реагирование", DdsStageType.COMPLETE_INCIDENT, 30, List.of());
        var incident = new DdsIncident(null, "Пожар", new Address("Москва", "Тверская", "1", null, null, 1),
                Difficulty.NORMAL, List.of(initialStage, successStage, failureStage),
                new PreparedCardTemplate(List.of("101", "102"), null, 0, java.util.Map.of()),
                new InitialAssignment(EmergencyService.FIRE, "101", "Направить ближайшую бригаду"),
                new DdsCriteria(List.of("Адрес?"), List.of("Назначить бригаду"), List.of()),
                initialStageId,
                List.of(new DdsStageTransition(initialStageId, successStageId, failureStageId)));

        var saved = incidentRepository.save(incident);
        var loaded = (DdsIncident) incidentRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.stages()).hasOnlyElementsOfType(DdsStage.class);
        assertThat(loaded.stages().getFirst().calls().getFirst().direction()).isEqualTo(CallDirection.OUTBOUND);
        assertThat(loaded.stages().getFirst().type()).isEqualTo(DdsStageType.CALL_BRIGADE_FOR_STATUS);
        assertThat(loaded.stages().getFirst().timeLimitSeconds()).isEqualTo(60);
        assertThat(detailCount("DDS_STAGE_DETAILS", saved.id())).isEqualTo(3);
        assertThat(loaded.transitions()).containsExactly(
                new DdsStageTransition(initialStageId, successStageId, failureStageId));
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
