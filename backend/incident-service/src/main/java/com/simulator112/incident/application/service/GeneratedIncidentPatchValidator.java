package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
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
                    || incident.has("stages") && (!incident.path("stages").isArray() || incident.path("stages").isEmpty() || incident.path("stages").size() > 15)
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
                        } else if (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < 0) {
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
                validateOptionalText(stage, "title", "description");
                if (!stage.path("calls").isArray() || !stage.path("classifierCodes").isArray()
                        || stage.path("classifierCodes").isEmpty() || stage.path("calls").isEmpty()
                        || stage.path("calls").size() > 10 || !stage.path("victimCount").isIntegralNumber() || !stage.path("victimCount").canConvertToInt()
                        || stage.path("victimCount").asInt() < 0) throw new IllegalStateException("Invalid stage");
                for (var code : stage.path("classifierCodes")) {
                    if (!code.isTextual() || !allowedCodes.contains(code.asText())) throw new IllegalStateException("Unknown classifier code");
                }
                for (var call : stage.path("calls")) {
                    var person = call.path("person");
                    for (var field : List.of("firstName", "lastName", "phone")) {
                        requireText(person.path(field));
                    }
                    validateOptionalText(person, "middleName", "contactPhone", "onScenePhone", "address", "additionalInfo");
                    if (person.hasNonNull("age") && (!person.path("age").isIntegralNumber()
                            || !person.path("age").canConvertToInt() || person.path("age").asInt() < 0)) {
                        throw new IllegalStateException("Invalid age");
                    }
                    validateFacts(call.path("knownFacts"), true);
                    if (call.hasNonNull("hiddenFacts")) validateFacts(call.path("hiddenFacts"), false);
                    validateOptionalText(call, "aiContext", "emotionalState");
                    if (call.hasNonNull("gender") && !List.of("MAN", "WOMEN").contains(call.path("gender").asText())
                            || call.has("direction") && !"INBOUND".equals(call.path("direction").asText())
                            || call.has("counterparty") && !"CALLER".equals(call.path("counterparty").asText())) {
                        throw new IllegalStateException("Invalid call fields");
                    }
                }
            }
            int totalWeight = 0;
            for (var criterion : incident.path("dialogueCriteria")) {
                requireText(criterion.path("name"));
                requireText(criterion.path("hypothesis"));
                if (!criterion.path("weight").isIntegralNumber() || !criterion.path("weight").canConvertToInt()) {
                    throw new IllegalStateException("Invalid criterion weight");
                }
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

    private void requireText(JsonNode value) {
        if (!value.isTextual() || value.asText().isBlank()) throw new IllegalStateException("Expected nonempty text");
    }

    private void validateOptionalText(JsonNode value, String... fields) {
        for (var field : fields) {
            if (value.hasNonNull(field) && !value.path(field).isTextual()) {
                throw new IllegalStateException("Invalid text field: " + field);
            }
        }
    }

    private void validateFacts(JsonNode values, boolean required) {
        if (!values.isArray() || required && values.isEmpty()) throw new IllegalStateException("Invalid facts");
        for (var value : values) requireText(value);
    }
}
