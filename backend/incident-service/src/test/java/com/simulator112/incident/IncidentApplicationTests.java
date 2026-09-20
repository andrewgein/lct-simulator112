package com.simulator112.incident;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "grpc.server.port=0")
class IncidentApplicationTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoadsWithoutClassifierTables() {
    Integer classifierTables = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CLASSIFIER_ENTRIES'",
        Integer.class);
    Integer classifierCodeColumns = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
            + "WHERE TABLE_NAME = 'STAGES' AND COLUMN_NAME = 'CLASSIFIER_CODE'",
        Integer.class);

    assertThat(classifierTables).isZero();
    assertThat(classifierCodeColumns).isEqualTo(1);
  }
}
