package com.simulator112.classifier;

import com.simulator112.classifier.application.port.in.ResolveRoutingUseCase;
import com.simulator112.classifier.adapter.in.web.ClassifierController;
import com.simulator112.classifier.domain.model.RoutingResultKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassifierApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ResolveRoutingUseCase routing;
    @Autowired
    private ClassifierController classifierController;
    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void exposesClassifierFeaturesAndRoutingFacts() {
        var view = classifierController.getClassifier();
        var entry = view.categories().stream()
                .flatMap(category -> category.entries().stream())
                .filter(item -> item.code().equals("1010101"))
                .findFirst()
                .orElseThrow();

        assertThat(entry.features()).isNotEmpty();
        assertThat(entry.routingFactCodes()).contains("ACCESS_STATUS");
        assertThat(view.routingFacts()).anySatisfy(fact -> {
            assertThat(fact.code()).isEqualTo("ACCESS_STATUS");
            assertThat(fact.options()).extracting("value").containsExactly("AVAILABLE", "NO_ACCESS");
        });
    }

    @Test
    void serializesNewClassifierContract() throws Exception {
        mockMvc.perform(get("/api/v1/classifier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.routingFacts").isArray())
                .andExpect(jsonPath("$.categories[0].entries[0].features").isArray())
                .andExpect(jsonPath("$.categories[0].entries[0].routingFactCodes").isArray())
                .andExpect(jsonPath("$.categories[0].entries[0].feature1Name").doesNotExist())
                .andExpect(jsonPath("$.categories[0].entries[0].fields").doesNotExist());
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
