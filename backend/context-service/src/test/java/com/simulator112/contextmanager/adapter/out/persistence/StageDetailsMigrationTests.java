package com.simulator112.contextmanager.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class StageDetailsMigrationTests {
    @Test
    void upgradesAppliedVersionTwelveWithExistingDdsStage() throws Exception {
        String url = "jdbc:h2:mem:context_timeline_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").target("12").load().migrate();
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO contexts (uuid, assignment_id, level_title, target_type, difficulty, execution_mode) VALUES ('"
                    + contextId + "', '" + UUID.randomUUID() + "', 'Тест', 'DDS', 'NORMAL', 'PARALLEL')");
            statement.executeUpdate("INSERT INTO incident_contexts (id, context_id, source_incident_id, position, status, initial_stage_id) VALUES ('"
                    + incidentId + "', '" + contextId + "', '" + UUID.randomUUID() + "', 0, 'ACTIVE', '" + stageId + "')");
            statement.executeUpdate("INSERT INTO stage_contexts (id, source_stage_id, incident_context_id, status) VALUES ('"
                    + stageId + "', '" + UUID.randomUUID() + "', '" + incidentId + "', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO dds_stage_contexts (stage_context_id, dds_stage_type, time_limit_seconds, expected_comment) VALUES ('"
                    + stageId + "', 'CALL_BRIGADE_FOR_STATUS', 90, 'Бригада прибыла')");
        }

        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT expected_comment, actual_status FROM dds_stage_contexts WHERE stage_context_id = '" + stageId + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("Бригада прибыла");
                assertThat(result.getString(2)).isNull();
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'INCIDENT_CONTEXTS' AND COLUMN_NAME = 'INITIAL_STAGE_ID'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CONTEXT_DDS_STAGE_TRANSITIONS'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }

    @Test
    void migratesExistingSystem112AndDdsStagesWithoutLosingDetails() throws Exception {
        String url = "jdbc:h2:mem:stage_split_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").target("11").load().migrate();
        UUID systemContext = UUID.randomUUID();
        UUID ddsContext = UUID.randomUUID();
        UUID systemIncident = UUID.randomUUID();
        UUID ddsIncident = UUID.randomUUID();
        UUID systemStage = UUID.randomUUID();
        UUID ddsStage = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE stage_contexts ADD COLUMN expected_comment TEXT");
            statement.executeUpdate("ALTER TABLE stage_contexts ADD COLUMN comment TEXT");
            for (var entry : java.util.Map.of(systemContext, "SYSTEM_112", ddsContext, "DDS").entrySet()) {
                statement.executeUpdate("INSERT INTO contexts (uuid, assignment_id, level_title, target_type, difficulty, execution_mode) VALUES ('"
                        + entry.getKey() + "', '" + UUID.randomUUID() + "', 'Тест', '" + entry.getValue() + "', 'NORMAL', 'PARALLEL')");
            }
            statement.executeUpdate("INSERT INTO incident_contexts (id, context_id, source_incident_id, position, status) VALUES ('"
                    + systemIncident + "', '" + systemContext + "', '" + UUID.randomUUID() + "', 0, 'ACTIVE'), ('"
                    + ddsIncident + "', '" + ddsContext + "', '" + UUID.randomUUID() + "', 0, 'ACTIVE')");
            statement.executeUpdate("INSERT INTO stage_contexts (id, source_stage_id, incident_context_id, status, victim_count) VALUES ('"
                    + systemStage + "', '" + UUID.randomUUID() + "', '" + systemIncident + "', 'PENDING', 2)");
            statement.executeUpdate("INSERT INTO stage_contexts (id, source_stage_id, incident_context_id, status, victim_count, dds_stage_type, time_limit_seconds, expected_comment, comment) VALUES ('"
                    + ddsStage + "', '" + UUID.randomUUID() + "', '" + ddsIncident
                    + "', 'ACTIVE', 0, 'CALL_BRIGADE_FOR_STATUS', 90, 'Бригада прибыла', 'Бригада на месте')");
            statement.executeUpdate("INSERT INTO stage_context_classifier_codes (stage_context_id, position, classifier_code) VALUES ('"
                    + systemStage + "', 0, '101')");
        }

        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT victim_count FROM system112_stage_contexts WHERE stage_context_id = '" + systemStage + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
            try (var result = statement.executeQuery("SELECT dds_stage_type, time_limit_seconds, expected_comment, comment FROM dds_stage_contexts WHERE stage_context_id = '" + ddsStage + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("CALL_BRIGADE_FOR_STATUS");
                assertThat(result.getInt(2)).isEqualTo(90);
                assertThat(result.getString(3)).isEqualTo("Бригада прибыла");
                assertThat(result.getString(4)).isEqualTo("Бригада на месте");
            }
            try (var result = statement.executeQuery("SELECT classifier_code FROM system112_stage_classifier_codes WHERE stage_context_id = '" + systemStage + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("101");
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'INCIDENT_CONTEXTS' AND COLUMN_NAME = 'INITIAL_STAGE_ID'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }
}
