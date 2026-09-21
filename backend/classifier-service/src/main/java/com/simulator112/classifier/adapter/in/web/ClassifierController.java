package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.ClassifierCategoryView;
import com.simulator112.classifier.adapter.in.web.dto.ResolveRoutingRequest;
import com.simulator112.classifier.adapter.in.web.dto.RoutingResultView;
import com.simulator112.classifier.application.port.in.GetClassifierUseCase;
import com.simulator112.classifier.application.port.in.ResolveRoutingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classifier")
@RequiredArgsConstructor
public class ClassifierController {

    private final GetClassifierUseCase classifier;
    private final ResolveRoutingUseCase routing;

    @GetMapping
    public List<ClassifierCategoryView> getClassifier() {
        return classifier.getClassifier().stream().map(ClassifierWebMapper::toView).toList();
    }

    @PostMapping("/{classifierCode}/routing")
    public RoutingResultView resolveRouting(
            @PathVariable String classifierCode,
            @Valid @RequestBody ResolveRoutingRequest request) {
        return ClassifierWebMapper.toView(routing.resolve(classifierCode, request.facts()));
    }
}
