package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GeneratedIncidentPatchValidator {
    private final ObjectMapper mapper;

    public GenerateIncidentDraftUseCase.Result validate(String modelContent, Set<String> allowedCodes) {
        try {
            var result = mapper.readTree(modelContent);
            var incident = result.path("incident");
            if (!incident.isObject() || !result.path("message").isTextual()
                    || !Set.of("title", "difficulty", "address", "stages", "dialogueCriteria")
                    .containsAll(incident.properties().stream().map(Map.Entry::getKey).toList())) {
                throw new IllegalStateException("Invalid incident patch");
            }
            if (incident.has("title") && !incident.path("title").isTextual()
                    || incident.has("difficulty") && !List.of("EASY", "NORMAL", "HARD").contains(incident.path("difficulty").asText())
                    || incident.has("address") && !incident.path("address").isObject()
                    || incident.has("stages") && (!incident.path("stages").isArray() || incident.path("stages").size() > 15)
                    || incident.has("dialogueCriteria") && (!incident.path("dialogueCriteria").isArray() || incident.path("dialogueCriteria").size() > 20)) {
                throw new IllegalStateException("Invalid incident fields");
            }
            if (incident.has("address")) {
                var address = incident.path("address");
                if (!Set.of("city", "street", "house", "building", "apartment", "floor")
                        .containsAll(address.properties().stream().map(Map.Entry::getKey).toList())) {
                    throw new IllegalStateException("Invalid address fields");
                }
                for (var field : address.properties()) {
                    var value = field.getValue();
                    if (value.isNull()) continue;
                    if (field.getKey().equals("floor")) {
                        if (value.isTextual()) {
                            var text = value.asText().trim();
                            if (text.isEmpty()) ((ObjectNode) address).putNull("floor");
                            else {
                                try {
                                    int floor = Integer.parseInt(text);
                                    if (floor < 0) throw new NumberFormatException();
                                    ((ObjectNode) address).put("floor", floor);
                                } catch (NumberFormatException e) {
                                    throw new IllegalStateException("Invalid address value: floor (expected nonnegative integer)", e);
                                }
                            }
                        } else if (!value.canConvertToInt() || value.asInt() < 0) {
                            throw new IllegalStateException("Invalid address value: floor (expected nonnegative integer)");
                        }
                    } else if (value.isNumber()) {
                        ((ObjectNode) address).put(field.getKey(), value.asText());
                    } else if (!value.isTextual()) {
                        throw new IllegalStateException("Invalid address value: " + field.getKey() + " (expected text)");
                    }
                }
            }
            for (var stage : incident.path("stages")) {
                if (!stage.path("calls").isArray() || !stage.path("classifierCodes").isArray()
                        || stage.path("classifierCodes").isEmpty() || stage.path("calls").isEmpty()
                        || stage.path("calls").size() > 10 || !stage.path("victimCount").canConvertToInt()
                        || stage.path("victimCount").asInt() < 0) throw new IllegalStateException("Invalid stage");
                for (var code : stage.path("classifierCodes")) {
                    if (!code.isTextual() || !allowedCodes.contains(code.asText())) throw new IllegalStateException("Unknown classifier code");
                }
                for (var call : stage.path("calls")) {
                    var person = call.path("person");
                    if (person.path("firstName").asText().isBlank() || person.path("lastName").asText().isBlank()
                            || person.path("phone").asText().isBlank() || !call.path("knownFacts").isArray()
                            || call.path("knownFacts").isEmpty()) throw new IllegalStateException("Incomplete call");
                }
            }
            int totalWeight = 0;
            for (var criterion : incident.path("dialogueCriteria")) {
                int weight = criterion.path("weight").asInt(0);
                if (weight < 1 || weight > 40) throw new IllegalStateException("Invalid criterion");
                totalWeight += weight;
            }
            if (totalWeight > 40) throw new IllegalStateException("Criteria exceed 40 points");
            return new GenerateIncidentDraftUseCase.Result(result.path("message").asText(), incident);
        } catch (IncidentGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось получить корректный ответ модели", e);
        }
    }
}
