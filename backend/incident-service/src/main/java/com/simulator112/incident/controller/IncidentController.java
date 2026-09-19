package com.simulator112.incident.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.simulator112.incident.dto.request.ResolveRoutingRequest;
import com.simulator112.incident.dto.view.ClassifierCategoryView;
import com.simulator112.incident.dto.view.RoutingResultView;
import com.simulator112.incident.service.ClassifierService;
import com.simulator112.incident.service.RoutingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final ClassifierService classifierService;
    private final RoutingService routingService;

    @GetMapping("/classifier")
    public List<ClassifierCategoryView> getClassifier() {

        return classifierService.getClassifier();
    }

    @PostMapping("/classifier/{classifierCode}/routing")
    public RoutingResultView resolveRouting(
            @PathVariable String classifierCode,
            @Valid @RequestBody ResolveRoutingRequest request
    ) {
        return routingService.resolve(classifierCode, request);
    }
}
