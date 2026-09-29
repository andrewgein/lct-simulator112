package com.simulator112.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class DdsStatusMigrationTests {
    @Test
    void convertsLegacyStatusesAndKeepsStagesCompletable() throws Exception {
        String url = "jdbc:h2:mem:incident_statuses_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername("sa");
        try (var dataSource = new HikariDataSource(config)) {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("15").load().migrate();
            UUID incidentId = UUID.randomUUID();
            UUID completedId = UUID.randomUUID();
            UUID statusOnlyId = UUID.randomUUID();
            UUID statusAndCallsId = UUID.randomUUID();
            UUID initialId = UUID.randomUUID();
            UUID validId = UUID.randomUUID();
            try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO incidents(id, title, target_type, difficulty, emergency_service) VALUES ('" + incidentId + "', 'Legacy', 'DDS', 'NORMAL', 'FIRE')");
                UUID[] stages = {completedId, statusOnlyId, statusAndCallsId, initialId, validId};
                String[] types = {"COMPLETE_INCIDENT", "WAIT_FOR_BRIGADE_STATUS_CHANGE", "CALL_BRIGADE_FOR_STATUS", "ASSIGN_BRIGADE", "COMPLETE_INCIDENT"};
                String[] statuses = {"COMPLETED", "PROCESSED", "VERIFIED", "REGISTERED", "WORK_COMPLETED"};
                for (int i = 0; i < stages.length; i++) {
                    statement.executeUpdate("INSERT INTO incident_stages(id, incident_id, position, title) VALUES ('" + stages[i] + "', '" + incidentId + "', " + i + ", 'Stage')");
                    statement.executeUpdate("INSERT INTO dds_stage_details(stage_id, stage_type, time_limit_seconds, actual_status) VALUES ('" + stages[i] + "', '" + types[i] + "', 30, '" + statuses[i] + "')");
                }
                statement.executeUpdate("INSERT INTO dds_stage_completion_triggers VALUES ('" + statusOnlyId + "', 'STATUS')");
                statement.executeUpdate("INSERT INTO dds_stage_completion_triggers VALUES ('" + statusAndCallsId + "', 'STATUS')");
                statement.executeUpdate("INSERT INTO dds_stage_completion_triggers VALUES ('" + statusAndCallsId + "', 'CALLS')");
                statement.executeUpdate("INSERT INTO dds_stage_completion_triggers VALUES ('" + initialId + "', 'STATUS')");
                statement.executeUpdate("INSERT INTO dds_stage_completion_triggers VALUES ('" + validId + "', 'TIME')");
            }
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
                assertThat(status(statement, completedId)).isEqualTo("WORK_COMPLETED");
                assertThat(status(statement, statusOnlyId)).isNull();
                assertThat(status(statement, statusAndCallsId)).isNull();
                assertThat(status(statement, initialId)).isNull();
                assertThat(status(statement, validId)).isEqualTo("WORK_COMPLETED");
                assertThat(triggers(statement, statusOnlyId)).containsExactly("TIME");
                assertThat(triggers(statement, statusAndCallsId)).containsExactly("CALLS");
                assertThat(triggers(statement, initialId)).containsExactly("STATUS");
                assertThat(triggers(statement, validId)).containsExactly("TIME");
            }
        }
    }

    private String status(java.sql.Statement statement, UUID stageId) throws Exception {
        try (var result = statement.executeQuery("SELECT actual_status FROM dds_stage_details WHERE stage_id = '" + stageId + "'")) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private java.util.List<String> triggers(java.sql.Statement statement, UUID stageId) throws Exception {
        try (var result = statement.executeQuery("SELECT completion_trigger FROM dds_stage_completion_triggers WHERE stage_id = '" + stageId + "' ORDER BY completion_trigger")) {
            var values = new java.util.ArrayList<String>();
            while (result.next()) values.add(result.getString(1));
            return values;
        }
    }
}
