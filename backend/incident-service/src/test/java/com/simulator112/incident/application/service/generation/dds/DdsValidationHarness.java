package com.simulator112.incident.application.service.generation.dds;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.service.GeneratedIncidentPatchValidator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;

/** Exercises DDS normalization and validation independently of the model's step selection. */
public final class DdsValidationHarness {
    private DdsValidationHarness() {}

    public static GenerateIncidentDraftUseCase.Result validate(String response, JsonNode draft, ObjectMapper mapper) {
        var catalog = new ClassifierCatalogPort() {
            @Override public void requireEntry(String code) {}
            @Override public void requireService(String code) {}
            @Override public List<String> resolveAssignedServices(List<String> codes) { return List.of("MCHS", "POLICE"); }
            @Override public List<Candidate> search(String query, int limit, List<String> codes) { return List.of(); }
        };
        var service = new DdsIncidentGenerationService(catalog, messages -> "",
                new GeneratedIncidentPatchValidator(mapper), mapper);
        var command = new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Правка")), draft);
        var codes = new LinkedHashMap<String, String>();
        codes.put("1050101", "Пожар");
        var services = new LinkedHashMap<String, String>();
        services.put("MCHS", "МЧС");
        services.put("POLICE", "Полиция");
        return service.validateResponse(mapper.readTree(response), codes, services, command);
    }
}
