package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.CreateLevelUseCase;
import com.simulator112.incident.application.port.in.GetLevelUseCase;
import com.simulator112.incident.application.port.in.UpdateLevelUseCase;
import com.simulator112.incident.domain.level.Level;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
public class LevelRestController {
    private final CreateLevelUseCase createLevel;
    private final UpdateLevelUseCase updateLevel;
    private final GetLevelUseCase getLevel;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Level create(@Valid @RequestBody LevelRequest request) {
        return createLevel.createLevel(toDomain(null, request));
    }

    @PutMapping("/{levelId}")
    public Level update(@PathVariable UUID levelId, @Valid @RequestBody LevelRequest request) {
        return updateLevel.updateLevel(levelId, toDomain(levelId, request));
    }

    @GetMapping("/{levelId}")
    public Level get(@PathVariable UUID levelId) {
        return getLevel.getLevel(levelId);
    }

    private Level toDomain(UUID id, LevelRequest request) {
        return new Level(id, request.title(), request.targetType(), request.difficulty(),
                request.executionMode(), request.incidentIds());
    }
}
