package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.ClassifierView;
import com.simulator112.classifier.adapter.in.web.dto.ResolveRoutingRequest;
import com.simulator112.classifier.adapter.in.web.dto.RoutingResultView;
import com.simulator112.classifier.application.port.in.GetClassifierUseCase;
import com.simulator112.classifier.application.port.in.ResolveRoutingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classifier")
@RequiredArgsConstructor
public class ClassifierController {

    private final GetClassifierUseCase classifier;
    private final ResolveRoutingUseCase routing;

    @GetMapping
    public ClassifierView getClassifier() {
        return ClassifierWebMapper.toView(classifier.getClassifier());
    }

    @PostMapping("/{classifierCode}/routing")
    public RoutingResultView resolveRouting(
            @PathVariable String classifierCode,
            @Valid @RequestBody ResolveRoutingRequest request) {
        return ClassifierWebMapper.toView(routing.resolve(classifierCode, request.facts()));
    }
}
