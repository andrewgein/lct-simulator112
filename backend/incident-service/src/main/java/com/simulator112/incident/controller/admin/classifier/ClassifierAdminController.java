package com.simulator112.incident.controller.admin.classifier;

import com.simulator112.incident.dto.view.classifier.ClassifierCategoryView;
import com.simulator112.incident.service.classifier.ClassifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/incident/classifier")
@RequiredArgsConstructor
public class ClassifierAdminController {

    private final ClassifierService classifierService;

    @GetMapping
    public List<ClassifierCategoryView> getClassifier() {
        return classifierService.getClassifier();
    }
}
