package com.simulator112.incident.controller;

import com.simulator112.incident.dto.view.LevelView;
import com.simulator112.incident.service.admin.LevelAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incident/levels")
@RequiredArgsConstructor
public class LevelController {

    private final LevelAdminService levelAdminService;

    @GetMapping
    public List<LevelView> getLevels() {
        return levelAdminService.getLevels();
    }
}
