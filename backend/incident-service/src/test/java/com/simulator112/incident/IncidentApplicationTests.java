package com.simulator112.incident;

import com.simulator112.incident.dto.request.ResolveRoutingRequest;
import com.simulator112.incident.model.enums.RoutingResultKind;
import com.simulator112.incident.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IncidentApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RoutingService routingService;

	@Test
	void contextLoads() {
		assertThat(count("classifier_categories")).isEqualTo(24);
		assertThat(count("classifier_entries")).isEqualTo(1283);
		assertThat(count("dispatch_services")).isEqualTo(58);
		assertThat(count("routing_variants")).isEqualTo(86);
		assertThat(count("routing_variant_conditions")).isEqualTo(28);
		assertThat(count("routing_rules")).isEqualTo(22484);
	}

	@Test
	void resolvesRoutingFromClassifierRules() {
		var result = routingService.resolve("1010101", new ResolveRoutingRequest(Map.of(
				"ACCESS_STATUS", "AVAILABLE",
				"OFFENSE_STATUS", "PRESENT",
				"VICTIM_STATUS", "PRESENT",
				"GASIFICATION", "TRUE",
				"THREAT_TO_PEOPLE", "TRUE"
		)));

		assertThat(result.classifierCode()).isEqualTo("1010101");
		assertThat(result.decisions()).anySatisfy(decision -> {
			assertThat(decision.service().code()).isEqualTo("MCHS");
			assertThat(decision.routingTarget()).isEqualTo("Служба 101");
			assertThat(decision.resultKind()).isEqualTo(RoutingResultKind.SERVICE_TYPE);
			assertThat(decision.targetTypeName()).isEqualTo("пожар: мусор");
		});
		assertThat(result.decisions()).anySatisfy(decision -> {
			assertThat(decision.service().code()).isEqualTo("AMBULANCE");
			assertThat(decision.resultKind()).isEqualTo(RoutingResultKind.SERVICE_TYPE);
			assertThat(decision.targetTypeName()).isEqualTo("пожар");
		});
		assertThat(result.decisions().stream()
				.filter(decision -> decision.service().code().equals("POLICE")))
				.singleElement()
				.satisfies(decision -> {
					assertThat(decision.matchedVariant()).isEqualTo("выбран признак Правонарушение");
					assertThat(decision.targetTypeName()).isEqualTo("пожар");
				});
		assertThat(result.decisions()).anySatisfy(decision -> {
			assertThat(decision.service().code()).isEqualTo("MOSGAZ");
			assertThat(decision.resultKind()).isEqualTo(RoutingResultKind.SERVICE_TYPE);
			assertThat(decision.targetTypeName()).isEqualTo("пожар");
		});
	}

	private Long count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
	}
}
