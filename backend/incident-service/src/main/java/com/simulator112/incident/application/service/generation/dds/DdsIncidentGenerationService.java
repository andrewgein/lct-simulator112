package com.simulator112.incident.application.service.generation.dds;

import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.GeneratedIncidentPatchValidator;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.generation.AbstractIncidentGenerationService;
import com.simulator112.incident.application.service.generation.EditableJsonStep;
import com.simulator112.incident.application.service.generation.GenerationStep;
import com.simulator112.incident.application.service.generation.StepPaths;
import com.simulator112.incident.application.service.generation.dds.steps.DdsBasicFieldsStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsIntermediateStageStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsPreparedCardStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsScenarioStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsStagePlanStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsValidationStep;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class DdsIncidentGenerationService extends AbstractIncidentGenerationService {
    private final DdsValidationStep validation;

    public DdsIncidentGenerationService(ClassifierCatalogPort classifierCatalog, IncidentLanguageModelPort model,
            GeneratedIncidentPatchValidator validator, ObjectMapper mapper) {
        super(classifierCatalog, model, validator, mapper);
        this.validation = new DdsValidationStep(classifierCatalog, validator, mapper);
    }

    @Override protected boolean dds() { return true; }

    @Override
    protected List<String> includedCodes(JsonNode draft) {
        var codes = new ArrayList<String>();
        for (var code : draft.path("preparedCardTemplate").path("classifierCodes")) {
            if (code.isTextual() && !code.asText().isBlank() && !codes.contains(code.asText()))
                codes.add(code.asText());
        }
        for (var code : super.includedCodes(draft)) if (!codes.contains(code)) codes.add(code);
        return codes;
    }

    @Override
    protected ClassifierLookup lookupClassifier(String query, List<String> includedCodes) {
        var lookup = super.lookupClassifier(query, includedCodes);
        if (lookup.failure() != null)
            throw new IncidentGenerationException("Не удалось получить коды классификатора", lookup.failure());
        if (lookup.candidates().isEmpty()) throw new IncidentGenerationException("No classifier candidates");
        return lookup;
    }

    @Override
    protected LinkedHashMap<String, String> availableServices(JsonNode draft) {
        var services = new LinkedHashMap<String, String>();
        for (var service : draft.path("availableServices")) {
            if (service.path("code").isTextual() && service.path("name").isTextual() && services.size() < 100)
                services.put(service.path("code").asText(), service.path("name").asText());
        }
        return services;
    }

    @Override
    protected JsonNode prepareDraft(JsonNode draft) {
        var modelDraft = draft.deepCopy();
        if (modelDraft instanceof ObjectNode object) {
            object.remove("availableServices");
            if (object.path("preparedCardTemplate") instanceof ObjectNode card) {
                card.remove("assignedServices");
                if (card.path("applicant") instanceof ObjectNode applicant
                        && applicant.properties().stream().allMatch(entry -> entry.getValue().isNull()
                                || entry.getValue().isTextual() && entry.getValue().asText().isBlank())) {
                    card.remove("applicant");
                }
                if (card.path("additionalInfo").isObject() && card.path("additionalInfo").isEmpty()) card.remove("additionalInfo");
            }
        }
        for (var stage : modelDraft.path("stages")) {
            if (stage instanceof ObjectNode object) object.remove("position");
            for (var call : stage.path("calls")) if (call instanceof ObjectNode object) object.remove("position");
        }
        return modelDraft;
    }

    @Override
    protected List<StepPaths> updateSteps() {
        return List.of(DdsBasicFieldsStep.PATHS, DdsPreparedCardStep.PATHS, DdsIntermediateStageStep.PATHS);
    }

    @Override
    protected ObjectNode updateSelectedFields(Command command, GenerationContext context,
            GenerationStep.Chain chain, List<String> paths) {
        var patch = mapper.createObjectNode();
        var codes = context.codes();
        var services = context.services();
        var validate = (java.util.function.Function<JsonNode, Result>)
                response -> validateResponse(response, codes, services, command);
        var requirements = command.messages().getLast().content();
        var scenario = command.draft().path("title").asText("") + ". " + requirements;
        var basicPaths = paths.stream().filter(DdsBasicFieldsStep.PATHS::matches).toList();
        if (!basicPaths.isEmpty()) {
            var basic = chain.then(new DdsBasicFieldsStep(mapper, scenario, requirements,
                    context.modelDraft(), validate, new EditableJsonStep.Edit(command.draft(), basicPaths)));
            mergePatch(patch, basic);
        }
        var cardPaths = paths.stream().filter(DdsPreparedCardStep.PATHS::matches).toList();
        if (!cardPaths.isEmpty()) {
            var card = chain.then(new DdsPreparedCardStep(mapper, scenario, context.modelDraft(),
                    requirements, codes, validate, new EditableJsonStep.Edit(command.draft(), cardPaths)));
            mergePatch(patch, card);
        }
        if (paths.stream().anyMatch(DdsIntermediateStageStep.PATHS::matches)) {
            var selected = (ArrayNode) command.draft().path("stages").deepCopy();
            var stageContext = context.modelDraft().deepCopy();
            if (stageContext.path("preparedCardTemplate") instanceof ObjectNode card) card.remove("assignedServices");
            var assignedService = command.draft().path("initialAssignment").path("emergencyService").asText("");
            if (paths.contains("/stages")) {
                var plan = chain.then(new DdsStagePlanStep(mapper, scenario, requirements));
                selected = mapper.createArrayNode();
                var oldStages = command.draft().path("stages");
                if (oldStages.size() > 0 && "ASSIGN_BRIGADE".equals(oldStages.get(0).path("type").asText()))
                    selected.add(oldStages.get(0).deepCopy());
                var previous = mapper.createArrayNode();
                for (int index = 0; index < plan.size(); index++) {
                    var stages = chain.then(new DdsIntermediateStageStep(mapper, index, plan, previous,
                            stageContext, codes, services, assignedService, requirements, scenario, validate));
                    var stage = stages.get(stages.size() - 2).deepCopy();
                    selected.add(stage);
                    previous.add(stage.deepCopy());
                }
                if (oldStages.size() > 1 && "COMPLETE_INCIDENT".equals(oldStages.get(oldStages.size() - 1).path("type").asText()))
                    selected.add(oldStages.get(oldStages.size() - 1).deepCopy());
            } else {
                var indexes = paths.stream().map(StepPaths::selectedIndex).distinct().sorted().toList();
                for (var index : indexes) {
                    var old = command.draft().path("stages").get(index);
                    var plan = mapper.createArrayNode().add(mapper.createObjectNode()
                            .put("type", old.path("type").asText()).put("goal", old.path("title").asText()));
                    var stagePaths = paths.stream().filter(path -> StepPaths.selectedIndex(path) == index).toList();
                    var workingDraft = command.draft().deepCopy();
                    if (workingDraft instanceof ObjectNode working) working.set("stages", selected.deepCopy());
                    var stagePatch = chain.then(new DdsIntermediateStageStep(mapper, 0, plan, mapper.createArrayNode(),
                            stageContext, codes, services, assignedService, requirements, scenario, validate,
                            new EditableJsonStep.Edit(workingDraft, stagePaths), index));
                    selected = (ArrayNode) stagePatch.path("stages").deepCopy();
                }
            }
            if (paths.contains("/stages"))
                mergePatch(patch, DdsIntermediateStageStep.updateTimeline(mapper, command.draft(), selected));
            else patch.set("stages", selected);
        }
        return patch;
    }

    @Override
    protected Result validateResponse(JsonNode response, LinkedHashMap<String, String> classifierNames,
            LinkedHashMap<String, String> services, Command command) {
        return validation.validate(response, classifierNames, services, command);
    }

    @Override
    protected boolean isNewDraft(JsonNode draft) {
        if (!draft.path("title").asText("").isBlank()) return false;
        for (var field : draft.path("address")) if (field.isTextual() && !field.asText().isBlank()
                || field.isNumber() && field.asInt() != 0) return false;
        var card = draft.path("preparedCardTemplate");
        for (var code : card.path("classifierCodes")) if (code.isTextual() && !code.asText().isBlank()) return false;
        if (card.path("victimCount").asInt(0) > 0 || !card.path("additionalInfo").isEmpty()) return false;
        for (var field : card.path("applicant")) if (field.isTextual() && !field.asText().isBlank()
                || field.isNumber() && field.asInt() != 0) return false;
        var stages = draft.path("stages");
        if (stages.size() == 0) return true;
        if (stages.size() != 1) return false;
        var first = stages.get(0);
        return "ASSIGN_BRIGADE".equals(first.path("type").asText())
                && first.path("calls").isEmpty()
                && "Получение карточки".equals(first.path("title").asText())
                && first.path("description").asText("").isBlank()
                && first.path("expectedComment").asText("").isBlank()
                && first.path("actualStatus").asText("").isBlank();
    }

    @Override
    protected Result generate(Command command, LinkedHashMap<String, String> codes,
            LinkedHashMap<String, String> services, JsonNode modelDraft) {
        var userRequirements = command.messages().stream().filter(message -> "user".equals(message.role()))
                .map(Message::content).reduce("", (left, right) -> left + "\n" + right);
        if (userRequirements.length() > 4000)
            userRequirements = userRequirements.substring(userRequirements.length() - 4000);

        var chain = new GenerationStep.Chain(model);
        var validate = (java.util.function.Function<JsonNode, Result>)
                response -> validateResponse(response, codes, services, command);
        String scenario = chain.then(new DdsScenarioStep(mapper, modelDraft, command.messages()));
        JsonNode planItems = chain.then(new DdsStagePlanStep(mapper, scenario, userRequirements));
        var incident = (ObjectNode) chain.then(new DdsBasicFieldsStep(mapper, scenario,
                userRequirements, modelDraft, validate));
        var card = chain.then(new DdsPreparedCardStep(mapper, scenario, incident,
                userRequirements, codes, validate));
        incident.set("preparedCardTemplate", card);

        var accepted = mapper.createArrayNode();
        var stageContext = incident.deepCopy();
        if (stageContext.path("preparedCardTemplate") instanceof ObjectNode preparedCard)
            preparedCard.remove("assignedServices");
        for (int index = 0; index < planItems.size(); index++) {
            var validated = chain.then(new DdsIntermediateStageStep(mapper, index, planItems,
                    accepted, stageContext, codes, services,
                    command.draft().path("initialAssignment").path("emergencyService").asText(""),
                    userRequirements, scenario, validate));
            accepted.add(validated.get(validated.size() - 2).deepCopy());
        }
        var complete = mapper.createObjectNode().put("message", "Готово");
        incident.set("stages", accepted);
        complete.set("incident", incident);
        return validateResponse(complete, codes, services, command);
    }

}
