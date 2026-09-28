package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.dds.DdsIncidentGenerationService;
import com.simulator112.incident.application.service.generation.system112.System112IncidentGenerationService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class IncidentDraftGenerationService implements GenerateIncidentDraftUseCase {
    private final DdsIncidentGenerationService ddsGenerator;
    private final System112IncidentGenerationService system112Generator;

    public IncidentDraftGenerationService(ClassifierCatalogPort classifierCatalog, IncidentLanguageModelPort model,
            GeneratedIncidentPatchValidator validator, ObjectMapper mapper) {
        this.ddsGenerator = new DdsIncidentGenerationService(classifierCatalog, model, validator, mapper);
        this.system112Generator = new System112IncidentGenerationService(classifierCatalog, model, validator, mapper);
    }

    @Override
    public Result generate(Command command) {
        if (command != null && command.draft() != null
                && "DDS".equals(command.draft().path("targetType").asText())) {
            return ddsGenerator.generate(command);
        }
        return system112Generator.generate(command);
    }
}
