package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.CreateIncidentUseCase;
import com.simulator112.incident.application.port.in.FindAvailableIncidentsUseCase;
import com.simulator112.incident.application.port.in.GetIncidentUseCase;
import com.simulator112.incident.application.port.in.ImportIncidentsUseCase;
import com.simulator112.incident.application.port.in.UpdateIncidentUseCase;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentRestController {
    private final CreateIncidentUseCase createIncident;
    private final UpdateIncidentUseCase updateIncident;
    private final GetIncidentUseCase getIncident;
    private final FindAvailableIncidentsUseCase findAvailableIncidents;
    private final ImportIncidentsUseCase importIncidents;
    private final IncidentSeedLoader seedLoader;
    private final IncidentRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incident create(@Valid @RequestBody IncidentRequest request) {
        return createIncident.createIncident(mapper.toDomain(null, request));
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentImportResponse importIncidents(@RequestParam(defaultValue = "true") boolean createLevels) {
        var imported = importIncidents.importIncidents(seedLoader.load(), createLevels);
        return new IncidentImportResponse(
                imported.incidents().stream().map(value -> new IncidentImportResponse.ImportedIncident(
                        value.id(), value.title(), value.targetType(), value.difficulty())).toList(),
                imported.levels().stream().map(value -> new IncidentImportResponse.ImportedLevel(
                        value.id(), value.title(), value.targetType(), value.difficulty(), value.incidentIds())).toList());
    }

    @PutMapping("/{incidentId}")
    public Incident update(@PathVariable UUID incidentId, @Valid @RequestBody IncidentRequest request) {
        return updateIncident.updateIncident(incidentId, mapper.toDomain(incidentId, request));
    }

    @GetMapping("/{incidentId}")
    public Incident get(@PathVariable UUID incidentId) {
        return getIncident.getIncident(incidentId);
    }

    @GetMapping
    public List<Incident> findAvailable(
            @RequestParam IncidentTargetType targetType,
            @RequestParam Difficulty difficulty) {
        return findAvailableIncidents.findAvailableIncidents(targetType, difficulty);
    }
}
