package com.simulator112.incident.controller.admin;

import com.simulator112.incident.dto.request.CreateStageRequest;
import com.simulator112.incident.dto.request.UpdateStageRequest;
import com.simulator112.incident.dto.view.StageView;
import com.simulator112.incident.service.admin.StageAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/incident")
@RequiredArgsConstructor
public class StageAdminController {

    private final StageAdminService stageAdminService;

    @PostMapping("/incidents/{incidentId}/stages")
    public StageView createStage(@PathVariable UUID incidentId, @Valid @RequestBody CreateStageRequest request) {

        return stageAdminService.createStage(incidentId, request);
    }

    @PatchMapping("/stages/{id}")
    public StageView updateStage(@PathVariable UUID id, @RequestBody UpdateStageRequest request) {

        return stageAdminService.updateStage(id, request);
    }

    @DeleteMapping("/stages/{id}")
    public void deleteStage(@PathVariable UUID id) {

        stageAdminService.deleteStage(id);
    }
}
