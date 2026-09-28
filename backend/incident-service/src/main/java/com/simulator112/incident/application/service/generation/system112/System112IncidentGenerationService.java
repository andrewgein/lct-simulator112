package com.simulator112.incident.application.service.generation.system112;

import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.ClassifierUnavailableException;
import com.simulator112.incident.application.service.GeneratedIncidentPatchValidator;
import com.simulator112.incident.application.service.generation.AbstractIncidentGenerationService;
import com.simulator112.incident.application.service.generation.EditableJsonStep;
import com.simulator112.incident.application.service.generation.GenerationStep;
import com.simulator112.incident.application.service.generation.StepPaths;
import com.simulator112.incident.application.service.generation.system112.steps.System112BasicFieldsStep;
import com.simulator112.incident.application.service.generation.system112.steps.System112CriteriaStep;
import com.simulator112.incident.application.service.generation.system112.steps.System112ScenarioStep;
import com.simulator112.incident.application.service.generation.system112.steps.System112StagePlanStep;
import com.simulator112.incident.application.service.generation.system112.steps.System112StageStep;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;

public final class System112IncidentGenerationService extends AbstractIncidentGenerationService {
    public System112IncidentGenerationService(ClassifierCatalogPort classifierCatalog, IncidentLanguageModelPort model,
            GeneratedIncidentPatchValidator validator, ObjectMapper mapper) {
        super(classifierCatalog, model, validator, mapper);
    }

    @Override protected boolean dds() { return false; }

    @Override protected boolean requiresClassifierForGeneration() { return true; }

    @Override
    protected List<StepPaths> updateSteps() {
        return List.of(System112BasicFieldsStep.PATHS, System112StageStep.PATHS, System112CriteriaStep.PATHS);
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
        var basicPaths = paths.stream().filter(System112BasicFieldsStep.PATHS::matches).toList();
        if (!basicPaths.isEmpty()) {
            var basic = chain.then(new System112BasicFieldsStep(mapper, scenario, requirements,
                    context.modelDraft(), validate, new EditableJsonStep.Edit(command.draft(), basicPaths)));
            mergePatch(patch, basic);
        }
        if (paths.stream().anyMatch(System112StageStep.PATHS::matches)) {
            if (codes.isEmpty()) throw new ClassifierUnavailableException(context.classifierFailure());
            var selected = (ArrayNode) command.draft().path("stages").deepCopy();
            if (paths.contains("/stages")) {
                var plan = chain.then(new System112StagePlanStep(mapper, scenario, requirements));
                selected = mapper.createArrayNode();
                for (int index = 0; index < plan.size(); index++) {
                    var stage = chain.then(new System112StageStep(mapper, index, plan, selected,
                            context.modelDraft(), codes, scenario, requirements, validate));
                    selected.add(stage.deepCopy());
                }
            } else {
                var indexes = paths.stream().map(StepPaths::selectedIndex).distinct().sorted().toList();
                for (var index : indexes) {
                    var old = command.draft().path("stages").get(index);
                    var plan = mapper.createArrayNode().add(mapper.createObjectNode().put("goal", old.path("title").asText()));
                    var stagePaths = paths.stream().filter(path -> StepPaths.selectedIndex(path) == index).toList();
                    var workingDraft = command.draft().deepCopy();
                    if (workingDraft instanceof ObjectNode working) working.set("stages", selected.deepCopy());
                    var stagePatch = chain.then(new System112StageStep(mapper, 0, plan, mapper.createArrayNode(),
                            context.modelDraft(), codes, scenario, requirements, validate,
                            new EditableJsonStep.Edit(workingDraft, stagePaths), index));
                    selected = (ArrayNode) stagePatch.path("stages").deepCopy();
                }
            }
            if (paths.contains("/stages"))
                mergePatch(patch, System112StageStep.updateTimeline(mapper, command.draft(), selected));
            else patch.set("stages", selected);
        }
        var criteriaPaths = paths.stream().filter(System112CriteriaStep.PATHS::matches).toList();
        if (!criteriaPaths.isEmpty()) {
            var criteria = chain.then(new System112CriteriaStep(mapper, scenario,
                    context.modelDraft(), requirements, validate,
                    new EditableJsonStep.Edit(command.draft(), criteriaPaths)));
            mergePatch(patch, criteria);
        }
        return patch;
    }

    @Override
    protected Result generate(Command command, LinkedHashMap<String, String> codes,
            LinkedHashMap<String, String> services, JsonNode modelDraft) {
        var chain = new GenerationStep.Chain(model, command.onStatus());
        var validate = (java.util.function.Function<JsonNode, Result>)
                response -> validateResponse(response, codes, services, command);
        String scenario = chain.then(new System112ScenarioStep(mapper, modelDraft, command.messages()));
        var requests = command.messages().stream().filter(message -> "user".equals(message.role()))
                .map(Message::content).reduce("", (left, right) -> left + "\n" + right);
        if (requests.length() > 4000) requests = requests.substring(requests.length() - 4000);

        JsonNode plan = chain.then(new System112StagePlanStep(mapper, scenario, requests));
        var incident = (ObjectNode) chain.then(new System112BasicFieldsStep(mapper,
                scenario, requests, modelDraft, validate));
        var stages = mapper.createArrayNode();
        for (int index = 0; index < plan.size(); index++) {
            JsonNode accepted = chain.then(new System112StageStep(mapper, index, plan, stages,
                    incident, codes, scenario, requests, validate));
            stages.add(accepted.deepCopy());
        }
        incident.set("stages", stages);
        var criteria = chain.then(new System112CriteriaStep(mapper, scenario, incident, requests, validate));
        incident.set("dialogueCriteria", criteria);
        var result = mapper.createObjectNode().put("message", "Готово");
        result.set("incident", incident);
        return validateResponse(result, codes, services, command);
    }

}
