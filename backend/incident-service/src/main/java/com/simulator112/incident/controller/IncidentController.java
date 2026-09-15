package com.simulator112.incident.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.simulator112.incident.dto.view.ClassifierTypeView;
import com.simulator112.incident.service.ClassifierService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final ClassifierService classifierService;

    @GetMapping("/classifier")
    public Map<String, Map<String, ClassifierTypeView>> getClassifier() {

        return classifierService.getClassifier();
    }
}
