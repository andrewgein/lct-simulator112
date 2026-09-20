package com.simulator112.classifier;

import com.simulator112.classifier.application.port.in.ResolveRoutingUseCase;
import com.simulator112.classifier.domain.model.RoutingResultKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ClassifierApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ResolveRoutingUseCase routing;

    @Test
    void loadsClassifierIntoDedicatedDatabase() {
        assertThat(count("classifier_categories")).isEqualTo(24);
        assertThat(count("classifier_entries")).isEqualTo(1283);
        assertThat(count("dispatch_services")).isEqualTo(58);
        assertThat(count("routing_rules")).isEqualTo(22484);
    }

    @Test
    void resolvesRouting() {
        var result = routing.resolve("1010101", Map.of(
                "ACCESS_STATUS", "AVAILABLE",
                "VICTIM_STATUS", "PRESENT",
                "GASIFICATION", "TRUE"));

        assertThat(result.decisions()).anySatisfy(decision -> {
            assertThat(decision.service().code()).isEqualTo("MCHS");
            assertThat(decision.resultKind()).isEqualTo(RoutingResultKind.SERVICE_TYPE);
        });
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
