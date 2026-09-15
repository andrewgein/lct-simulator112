package com.simulator112.incident.controller.admin;

import com.simulator112.incident.dto.request.CreateDialupRequest;
import com.simulator112.incident.dto.request.UpdateDialupRequest;
import com.simulator112.incident.dto.view.DialupView;
import com.simulator112.incident.service.admin.DialupAdminService;

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
public class DialupAdminController {

    private final DialupAdminService dialupAdminService;

    @PostMapping("/stages/{stageId}/dialups")
    public DialupView createDialup(@PathVariable UUID stageId, @Valid @RequestBody CreateDialupRequest request) {

        return dialupAdminService.createDialup(stageId, request);
    }

    @PatchMapping("/dialups/{id}")
    public DialupView updateDialup(@PathVariable UUID id, @Valid @RequestBody UpdateDialupRequest request) {

        return dialupAdminService.updateDialup(id, request);
    }

    @DeleteMapping("/dialups/{id}")
    public void deleteDialup(@PathVariable UUID id) {

        dialupAdminService.deleteDialup(id);
    }
}
