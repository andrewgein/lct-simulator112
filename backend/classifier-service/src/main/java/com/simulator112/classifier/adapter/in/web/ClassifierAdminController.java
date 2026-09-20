package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.ClassifierCategoryView;
import com.simulator112.classifier.application.port.in.GetClassifierUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/classifier")
@RequiredArgsConstructor
public class ClassifierAdminController {

    private final GetClassifierUseCase classifier;

    @GetMapping
    public List<ClassifierCategoryView> getClassifier() {
        return classifier.getClassifier().stream().map(ClassifierWebMapper::toView).toList();
    }
}
