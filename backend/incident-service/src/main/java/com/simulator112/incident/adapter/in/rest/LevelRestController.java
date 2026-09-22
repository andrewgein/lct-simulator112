package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GetLevelUseCase;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.Level;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
public class LevelRestController {
    private final GetLevelUseCase getLevel;

    @GetMapping
    public List<LevelCatalogResponse> getAll(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Training-Track", required = false) IncidentTargetType trainingTrack) {
        if (!"ADMIN".equals(role) && trainingTrack == null) {
            throw new IllegalArgumentException("Профиль обучения не назначен");
        }
        return getLevel.getLevels("ADMIN".equals(role) ? null : trainingTrack).stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    @GetMapping("/{levelId}")
    public LevelCatalogResponse get(
            @PathVariable UUID levelId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Training-Track", required = false) IncidentTargetType trainingTrack) {
        Level level = getLevel.getLevel(levelId);
        if (!"ADMIN".equals(role) && level.targetType() != trainingTrack) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Уровень недоступен для профиля обучения");
        }
        return toCatalogResponse(level);
    }

    private LevelCatalogResponse toCatalogResponse(Level level) {
        return new LevelCatalogResponse(level.id(), level.title(), level.targetType(), level.difficulty(),
                level.executionMode(), level.incidentIds().size());
    }
}
