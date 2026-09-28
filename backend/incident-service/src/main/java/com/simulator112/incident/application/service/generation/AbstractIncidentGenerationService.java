package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.service.ClassifierUnavailableException;
import com.simulator112.incident.application.service.GeneratedIncidentPatchValidator;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class AbstractIncidentGenerationService implements GenerateIncidentDraftUseCase {
    protected final ClassifierCatalogPort classifierCatalog;
    protected final IncidentLanguageModelPort model;
    protected final GeneratedIncidentPatchValidator validator;
    protected final ObjectMapper mapper;

    protected AbstractIncidentGenerationService(ClassifierCatalogPort classifierCatalog,
            IncidentLanguageModelPort model, GeneratedIncidentPatchValidator validator, ObjectMapper mapper) {
        this.classifierCatalog = classifierCatalog;
        this.model = model;
        this.validator = validator;
        this.mapper = mapper;
    }

    protected abstract boolean dds();
    protected abstract boolean isNewDraft(JsonNode draft);
    protected abstract Result generate(Command command, LinkedHashMap<String, String> codes,
            LinkedHashMap<String, String> services, JsonNode modelDraft);
    protected abstract List<StepPaths> updateSteps();
    protected abstract ObjectNode updateSelectedFields(Command command, GenerationContext context,
            GenerationStep.Chain chain, List<String> paths);

    @Override
    public final Result generate(Command command) {
        validateRequest(command);
        var chain = new GenerationStep.Chain(model, command.onStatus());
        var decision = chain.then(new IncidentIntentStep(mapper, command.draft(), command.messages()));
        if (decision.answer()) {
            var answer = chain.then(new IncidentAnswerStep(mapper, command.draft(), command.messages()));
            return new Result(answer, mapper.createObjectNode());
        }
        var context = prepare(command);
        try {
            if (isNewDraft(command.draft())) {
                if (requiresClassifierForGeneration() && context.codes().isEmpty())
                    throw new ClassifierUnavailableException(context.classifierFailure());
                return generate(command, context.codes(), context.services(), context.modelDraft());
            }
            return update(command, context);
        } catch (IncidentGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось подготовить запрос генерации", e);
        }
    }

    protected boolean requiresClassifierForGeneration() { return false; }

    private Result update(Command command, GenerationContext context) {
        var chain = new GenerationStep.Chain(model, command.onStatus());
        var paths = chain.then(new PatchSelectionStep(mapper, context.modelDraft(), command.messages(), dds(), updateSteps()));
        var incident = updateSelectedFields(command, context, chain, paths);
        var envelope = mapper.createObjectNode().put("message", "Готово");
        envelope.set("incident", incident);
        return validateResponse(envelope, context.codes(), context.services(), command);
    }

    protected JsonNode prepareDraft(JsonNode draft) {
        return draft.deepCopy();
    }

    protected List<String> includedCodes(JsonNode draft) {
        var codes = new ArrayList<String>();
        for (var stage : draft.path("stages")) for (var code : stage.path("classifierCodes")) {
            if (code.isTextual() && !codes.contains(code.asText())) codes.add(code.asText());
        }
        return codes;
    }

    protected ClassifierLookup lookupClassifier(String query, List<String> includedCodes) {
        try {
            return new ClassifierLookup(classifierCatalog.search(query, 40, includedCodes), null);
        } catch (Exception e) {
            return new ClassifierLookup(List.of(), e);
        }
    }

    public record ClassifierLookup(List<ClassifierCatalogPort.Candidate> candidates, Exception failure) {}

    protected LinkedHashMap<String, String> availableServices(JsonNode draft) {
        return new LinkedHashMap<>();
    }

    protected GenerationContext prepare(Command command) {
        validateRequest(command);
        var searchQuery = command.messages().stream().filter(message -> "user".equals(message.role()))
                .map(Message::content).reduce("", (left, right) -> left + " " + right);
        searchQuery += " " + command.draft().path("title").asText("");
        if (searchQuery.length() > 4000) searchQuery = searchQuery.substring(searchQuery.length() - 4000);
        var lookup = lookupClassifier(searchQuery, includedCodes(command.draft()));
        var classifierNames = new LinkedHashMap<String, String>();
        for (var candidate : lookup.candidates()) {
            classifierNames.put(candidate.code(), candidate.categoryName() + ": " + candidate.finalName());
        }
        try {
            var services = availableServices(command.draft());
            var modelDraft = prepareDraft(command.draft());
            return new GenerationContext(classifierNames, lookup.failure(), services, modelDraft);
        } catch (IncidentGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось подготовить запрос генерации", e);
        }
    }

    protected record GenerationContext(LinkedHashMap<String, String> codes, Exception classifierFailure,
            LinkedHashMap<String, String> services, JsonNode modelDraft) {}

    protected static void mergePatch(ObjectNode target, JsonNode block) {
        if (!(block instanceof ObjectNode object)) throw new IllegalStateException("Invalid generated patch block");
        for (var entry : object.properties()) target.set(entry.getKey(), entry.getValue().deepCopy());
    }

    protected Result validateResponse(JsonNode response, LinkedHashMap<String, String> classifierNames,
            LinkedHashMap<String, String> services, Command command) {
        return validator.validate(mapper.writeValueAsString(response), classifierNames.keySet(),
                services.keySet(), dds(), command.draft());
    }

    private void validateRequest(Command command) {
        if (command == null || command.messages() == null || command.messages().isEmpty() || command.messages().size() > 20
                || command.draft() == null || !command.draft().isObject()
                || command.messages().stream().anyMatch(m -> m == null || !List.of("user", "assistant").contains(m.role())
                    || m.content() == null || m.content().length() > 4000)
                || !"user".equals(command.messages().getLast().role())) {
            throw new IllegalArgumentException("Некорректный запрос к генератору");
        }
    }
}
