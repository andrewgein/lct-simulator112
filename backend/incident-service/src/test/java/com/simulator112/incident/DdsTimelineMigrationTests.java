package com.simulator112.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class DdsTimelineMigrationTests {
    @Test
    void upgradesAppliedVersionSevenSchema() throws Exception {
        String url = "jdbc:h2:mem:incident_timeline_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").target("7").load().migrate();
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DDS_STAGE_DETAILS' AND COLUMN_NAME = 'ACTUAL_STATUS'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'INCIDENTS' AND COLUMN_NAME = 'DDS_INITIAL_STAGE_ID'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'DDS_STAGE_TRANSITIONS'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }
}
