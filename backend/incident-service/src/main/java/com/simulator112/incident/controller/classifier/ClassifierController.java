package com.simulator112.incident.controller.classifier;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.simulator112.incident.dto.request.classifier.ResolveRoutingRequest;
import com.simulator112.incident.dto.view.classifier.ClassifierCategoryView;
import com.simulator112.incident.dto.view.classifier.RoutingResultView;
import com.simulator112.incident.service.classifier.ClassifierService;
import com.simulator112.incident.service.classifier.RoutingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/incident/classifier")
@RequiredArgsConstructor
public class ClassifierController {

    private final ClassifierService classifierService;
    private final RoutingService routingService;

    @GetMapping
    public List<ClassifierCategoryView> getClassifier() {

        return classifierService.getClassifier();
    }

    @PostMapping("/{classifierCode}/routing")
    public RoutingResultView resolveRouting(
            @PathVariable String classifierCode,
            @Valid @RequestBody ResolveRoutingRequest request
    ) {
        return routingService.resolve(classifierCode, request);
    }
}
