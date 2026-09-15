package com.simulator112.incident.controller.admin;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.simulator112.incident.dto.request.CreateIncidentRequest;
import com.simulator112.incident.dto.request.UpdateIncidentRequest;
import com.simulator112.incident.dto.view.ClassifierTypeView;
import com.simulator112.incident.dto.view.IncidentFullView;
import com.simulator112.incident.service.ClassifierService;
import com.simulator112.incident.service.admin.IncidentAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/incident")
@RequiredArgsConstructor
public class IncidentAdminController {

    private final IncidentAdminService incidentAdminService;
    private final ClassifierService classifierService;

    @GetMapping
    public Page<IncidentFullView> getAllIncidents(Pageable pageable) {

        return incidentAdminService.getAllIncidents(pageable);
    }

    @GetMapping("/{incidentId}")
    public IncidentFullView getFullIncidentById(@PathVariable UUID incidentId) {

        return incidentAdminService.getFullIncidentById(incidentId);
    }

    @PostMapping
    public IncidentFullView createIncident(@Valid @RequestBody CreateIncidentRequest request) {

        return incidentAdminService.createIncident(request);
    }

    @PatchMapping("/{incidentId}")
    public IncidentFullView updateIncident(@PathVariable UUID incidentId, @RequestBody UpdateIncidentRequest request) {

        return incidentAdminService.updateIncident(incidentId, request);
    }

    @DeleteMapping("/{incidentId}")
    public void deleteIncident(@PathVariable UUID incidentId) {

        incidentAdminService.deleteIncident(incidentId);
    }

    @GetMapping("/classifier")
    public Map<String, Map<String, ClassifierTypeView>> getClassifier() {

        return classifierService.getClassifier();
    }
}
