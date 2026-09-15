package com.simulator112.incident.controller.admin;

import com.simulator112.incident.dto.request.CreateLevelRequest;
import com.simulator112.incident.dto.request.UpdateLevelRequest;
import com.simulator112.incident.dto.view.LevelView;
import com.simulator112.incident.service.admin.LevelAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/incident/levels")
@RequiredArgsConstructor
public class LevelAdminController {

    private final LevelAdminService levelAdminService;

    @GetMapping
    public List<LevelView> getLevels() {
        return levelAdminService.getLevels();
    }

    @GetMapping("/{id}")
    public LevelView getLevel(@PathVariable UUID id) {
        return levelAdminService.getLevel(id);
    }

    @PostMapping
    public LevelView createLevel(@Valid @RequestBody CreateLevelRequest request) {
        return levelAdminService.createLevel(request);
    }

    @PatchMapping("/{id}")
    public LevelView updateLevel(@PathVariable UUID id, @RequestBody UpdateLevelRequest request) {
        return levelAdminService.updateLevel(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteLevel(@PathVariable UUID id) {
        levelAdminService.deleteLevel(id);
    }

    @PutMapping("/{levelId}/incidents/{incidentId}")
    public LevelView attachIncident(@PathVariable UUID levelId, @PathVariable UUID incidentId) {
        return levelAdminService.attachIncident(levelId, incidentId);
    }

    @DeleteMapping("/{levelId}/incidents/{incidentId}")
    public void detachIncident(@PathVariable UUID levelId, @PathVariable UUID incidentId) {
        levelAdminService.detachIncident(levelId, incidentId);
    }
}
