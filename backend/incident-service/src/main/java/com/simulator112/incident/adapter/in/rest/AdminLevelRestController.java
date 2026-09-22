package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.CreateLevelUseCase;
import com.simulator112.incident.application.port.in.DeleteLevelUseCase;
import com.simulator112.incident.application.port.in.GetIncidentUseCase;
import com.simulator112.incident.application.port.in.GetLevelUseCase;
import com.simulator112.incident.application.port.in.UpdateLevelUseCase;
import com.simulator112.incident.domain.level.Level;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/incident/levels")
@RequiredArgsConstructor
public class AdminLevelRestController {
    private final CreateLevelUseCase createLevel;
    private final UpdateLevelUseCase updateLevel;
    private final DeleteLevelUseCase deleteLevel;
    private final GetLevelUseCase getLevel;
    private final GetIncidentUseCase getIncident;

    @GetMapping
    public List<AdminLevelResponse> getAll() {
        return getLevel.getLevels(null).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{levelId}")
    public AdminLevelResponse get(@PathVariable UUID levelId) {
        return toResponse(getLevel.getLevel(levelId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminLevelResponse create(@Valid @RequestBody LevelRequest request) {
        return toResponse(createLevel.createLevel(toDomain(null, request)));
    }

    @PutMapping("/{levelId}")
    public AdminLevelResponse update(
            @PathVariable UUID levelId,
            @Valid @RequestBody LevelRequest request) {
        return toResponse(updateLevel.updateLevel(levelId, toDomain(levelId, request)));
    }

    @DeleteMapping("/{levelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID levelId) {
        deleteLevel.deleteLevel(levelId);
    }

    private Level toDomain(UUID id, LevelRequest request) {
        return new Level(id, request.title(), request.targetType(), request.difficulty(),
                request.executionMode(), request.incidentIds());
    }

    private AdminLevelResponse toResponse(Level level) {
        return new AdminLevelResponse(level.id(), level.title(), level.targetType(), level.difficulty(),
                level.executionMode(), level.incidentIds().stream().map(getIncident::getIncident).toList());
    }
}
