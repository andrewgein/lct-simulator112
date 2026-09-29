package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GeneratedIncidentPatchValidator {
    private final ObjectMapper mapper;

    private static final Set<String> DDS_STAGE_TYPES = Set.of("ASSIGN_BRIGADE", "WAIT_FOR_BRIGADE_STATUS_CHANGE",
            "CALL_BRIGADE_FOR_STATUS", "REQUEST_ADDITIONAL_SERVICE", "COMPLETE_INCIDENT");
    private static final Set<String> DDS_STATUSES = Set.of("ADDED", "RECEIVED_BY_SERVICE", "ACCEPTED",
            "NOT_ACCEPTED", "RESPONSE_STARTED", "ARRIVED", "WORK_IN_PROGRESS", "WORK_COMPLETED",
            "WORK_REFUSED");

    private static void onlyFields(JsonNode value, Set<String> fields) {
        if (!value.isObject()) throw new IllegalStateException("Invalid DDS object");
        var unknown = value.properties().stream().map(Map.Entry::getKey).filter(key -> !fields.contains(key)).toList();
        if (!unknown.isEmpty()) throw new IllegalStateException("Invalid DDS fields: " + unknown);
    }

    private static void person(JsonNode value) {
        if (value.isNull()) return;
        onlyFields(value, Set.of("firstName", "lastName", "middleName", "age", "phone", "contactPhone",
                "onScenePhone", "address", "additionalInfo"));
        for (var field : value.properties()) {
            if (field.getValue().isNull()) continue;
            if (field.getKey().equals("age") ? !field.getValue().canConvertToInt() || field.getValue().asInt() < 0
                    : !field.getValue().isTextual())
                throw new IllegalStateException("Invalid person field: " + field.getKey());
        }
    }

    private static boolean strings(JsonNode value) {
        if (!value.isArray()) return false;
        for (var item : value) if (!item.isTextual()) return false;
        return true;
    }

    private static void validateDds(JsonNode incident, Set<String> codes, Set<String> services, JsonNode draft) {
        String assignedService = draft.path("initialAssignment").path("emergencyService").asText();
        var existingStages = new java.util.HashSet<String>();
        var existingCalls = new java.util.HashSet<String>();
        for (var stage : draft.path("stages")) {
            if (stage.path("id").isTextual()) existingStages.add(stage.path("id").asText());
            for (var call : stage.path("calls")) if (call.path("id").isTextual()) existingCalls.add(call.path("id").asText());
        }
        var returnedStages = new java.util.HashSet<String>();
        var returnedCalls = new java.util.HashSet<String>();
        var card = incident.path("preparedCardTemplate");
        if (!card.isMissingNode()) {
            onlyFields(card, Set.of("classifierCodes", "applicant", "victimCount", "additionalInfo", "assignedServices"));
            if (card.has("classifierCodes")) {
                var entries = card.path("classifierCodes");
                if (!entries.isArray() || entries.isEmpty() || entries.size() > 15) throw new IllegalStateException("Invalid card types");
                for (var code : entries) if (!code.isTextual() || !codes.contains(code.asText()))
                    throw new IllegalStateException("Unknown classifier code");
            }
            if (card.has("assignedServices")) {
                var assigned = card.path("assignedServices");
                if (!strings(assigned) || assigned.size() > services.size() || assigned.size() > 100)
                    throw new IllegalStateException("Invalid assigned services");
                var seen = new java.util.HashSet<String>();
                for (var service : assigned) if (!services.contains(service.asText()) || !seen.add(service.asText()))
                    throw new IllegalStateException("Unknown or duplicate assigned service");
            }
            if (card.has("applicant")) person(card.path("applicant"));
            if (card.has("victimCount") && (!card.path("victimCount").canConvertToInt() || card.path("victimCount").asInt() < 0))
                throw new IllegalStateException("Invalid victim count");
            if (card.has("additionalInfo")) {
                var info = card.path("additionalInfo");
                if (!info.isObject() || info.properties().stream().anyMatch(entry -> !entry.getValue().isTextual()))
                    throw new IllegalStateException("Invalid card details");
            }
        }
        var assignment = incident.path("initialAssignment");
        if (!assignment.isMissingNode()) {
            onlyFields(assignment, Set.of("emergencyService"));
            if (assignment.has("emergencyService")) {
                if (!assignment.path("emergencyService").isTextual()
                        || !services.contains(assignment.path("emergencyService").asText()))
                    throw new IllegalStateException("Unknown dispatch service");
                assignedService = assignment.path("emergencyService").asText();
            }
        }
        var stages = incident.path("stages");
        if (stages.isMissingNode()) return;
        if (stages.size() < 2 || !"ASSIGN_BRIGADE".equals(stages.get(0).path("type").asText())
                || !"COMPLETE_INCIDENT".equals(stages.get(stages.size() - 1).path("type").asText()))
            throw new IllegalStateException("Invalid DDS stage boundaries");
        for (int index = 0; index < stages.size(); index++) {
            var stage = stages.get(index);
            onlyFields(stage, Set.of("id", "title", "description", "type", "timeLimitSeconds", "actualStatus", "expectedComment", "calls"));
            if (stage.hasNonNull("id") && (!stage.path("id").isTextual()
                    || !existingStages.contains(stage.path("id").asText()) || !returnedStages.add(stage.path("id").asText())))
                throw new IllegalStateException("Unknown DDS stage id");
            String stageLocation = "DDS stage " + (index + 1) + ": ";
            if (!stage.path("title").isTextual() || stage.path("title").asText().isBlank())
                throw new IllegalStateException(stageLocation + "title must be nonempty text");
            if (!DDS_STAGE_TYPES.contains(stage.path("type").asText())
                    || index > 0 && "ASSIGN_BRIGADE".equals(stage.path("type").asText()))
                throw new IllegalStateException(stageLocation + "invalid type");
            if (!stage.path("timeLimitSeconds").canConvertToInt() || stage.path("timeLimitSeconds").asInt() <= 0
                    || index == 0 && stage.path("timeLimitSeconds").asInt() != 30)
                throw new IllegalStateException(stageLocation + "timeLimitSeconds must be positive (first stage: 30)");
            if (!stage.path("calls").isArray() || stage.path("calls").size() > 10)
                throw new IllegalStateException(stageLocation + "calls must be an array of at most 10");
            if (stage.hasNonNull("description") && !stage.path("description").isTextual())
                throw new IllegalStateException(stageLocation + "description must be text or null");
            if (stage.hasNonNull("actualStatus") && !DDS_STATUSES.contains(stage.path("actualStatus").asText()))
                throw new IllegalStateException(stageLocation + "invalid actualStatus");
            if (stage.hasNonNull("expectedComment") && !stage.path("expectedComment").isTextual())
                throw new IllegalStateException(stageLocation + "expectedComment must be text or null");
            if (stage.hasNonNull("expectedComment") && !stage.path("expectedComment").asText().isBlank()
                    && stage.path("calls").isEmpty())
                throw new IllegalStateException(stageLocation + "expectedComment requires a call");
            int callIndex = 0;
            for (var call : stage.path("calls")) {
                callIndex++;
                var location = "DDS stage " + (index + 1) + " call " + callIndex + ": ";
                try {
                    onlyFields(call, Set.of("id", "direction", "counterparty", "serviceCode", "person", "gender",
                            "knownFacts", "hiddenFacts", "aiContext", "emotionalState"));
                    if (call.hasNonNull("id") && (!call.path("id").isTextual()
                            || !existingCalls.contains(call.path("id").asText()) || !returnedCalls.add(call.path("id").asText())))
                        throw new IllegalStateException("unknown or duplicate id");
                    if (!Set.of("INBOUND", "OUTBOUND").contains(call.path("direction").asText()))
                        throw new IllegalStateException("direction must be INBOUND or OUTBOUND");
                    if (!Set.of("BRIGADE", "SERVICE").contains(call.path("counterparty").asText()))
                        throw new IllegalStateException("counterparty must be BRIGADE or SERVICE");
                    if (call.has("person") && !call.path("person").isNull() && !call.path("person").isObject())
                        throw new IllegalStateException("person must be an object or null");
                    if (call.has("knownFacts") && !strings(call.path("knownFacts")))
                        throw new IllegalStateException("knownFacts must be an array of strings");
                    if (call.has("hiddenFacts") && !strings(call.path("hiddenFacts")))
                        throw new IllegalStateException("hiddenFacts must be an array of strings");
                    for (var field : List.of("aiContext", "emotionalState")) {
                        if (call.hasNonNull(field) && !call.path(field).isTextual())
                            throw new IllegalStateException(field + " must be text or null");
                    }
                    if (call.hasNonNull("gender") && !Set.of("MAN", "WOMEN").contains(call.path("gender").asText()))
                        throw new IllegalStateException("gender must be MAN, WOMEN or null");
                    if (call.has("person")) person(call.path("person"));
                    if ("SERVICE".equals(call.path("counterparty").asText())
                            && (!call.path("serviceCode").isTextual() || !services.contains(call.path("serviceCode").asText())
                            || call.path("serviceCode").asText().equals(assignedService)))
                        throw new IllegalStateException("serviceCode must name another available service");
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(location + e.getMessage(), e);
                }
            }
        }
    }

    public GenerateIncidentDraftUseCase.Result validate(String modelContent, Set<String> allowedCodes,
            Set<String> allowedServices, boolean dds, JsonNode draft) {
        try {
            var result = mapper.readTree(modelContent);
            var incident = result.path("incident");
            if (!incident.isObject() || !result.path("message").isTextual())
                throw new IllegalStateException("Invalid incident patch");
            var allowedFields = dds ? Set.of("title", "difficulty", "address", "preparedCardTemplate", "initialAssignment", "stages")
                    : Set.of("title", "difficulty", "address", "stages", "dialogueCriteria");
            var unknownFields = incident.properties().stream().map(Map.Entry::getKey)
                    .filter(key -> !allowedFields.contains(key)).toList();
            if (!unknownFields.isEmpty()) throw new IllegalStateException("Unknown incident fields: " + unknownFields);
            if (incident.has("title") && !incident.path("title").isTextual()
                    || incident.has("difficulty") && !List.of("EASY", "NORMAL", "HARD").contains(incident.path("difficulty").asText())
                    || incident.has("address") && !incident.path("address").isObject()
                    || incident.has("stages") && (!incident.path("stages").isArray() || incident.path("stages").isEmpty() || incident.path("stages").size() > 15)
                    || !dds && incident.has("dialogueCriteria") && (!incident.path("dialogueCriteria").isArray() || incident.path("dialogueCriteria").size() > 20)) {
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
            if (dds) {
                validateDds(incident, allowedCodes, allowedServices, draft);
                return new GenerateIncidentDraftUseCase.Result(result.path("message").asText(), incident);
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
