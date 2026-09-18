package com.simulator112.incident;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IncidentApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		assertThat(count("classifier_categories")).isEqualTo(24);
		assertThat(count("classifier_entries")).isEqualTo(1283);
		assertThat(count("dispatch_services")).isEqualTo(58);
		assertThat(count("routing_variants")).isEqualTo(86);
		assertThat(count("routing_variant_conditions")).isEqualTo(28);
		assertThat(count("routing_rules")).isEqualTo(22484);
	}

	private Long count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
	}
}
