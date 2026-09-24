package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.ClassifierView;
import com.simulator112.classifier.application.port.in.GetClassifierUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/classifier")
@RequiredArgsConstructor
public class ClassifierAdminController {

    private final GetClassifierUseCase classifier;

    @GetMapping
    public ClassifierView getClassifier() {
        return ClassifierWebMapper.toView(classifier.getClassifier());
    }
}
