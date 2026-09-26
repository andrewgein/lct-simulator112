package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.application.port.in.GetAnalyticsCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course-analytics")
@RequiredArgsConstructor
public class CourseAnalyticsRestController {
    private final GetAnalyticsCatalogUseCase analyticsCatalog;

    @GetMapping("/catalog")
    public GetAnalyticsCatalogUseCase.AnalyticsCatalog getCatalog(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role) && !"SUPERVISOR".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Аналитика доступна преподавателям");
        }
        return analyticsCatalog.getAnalyticsCatalog(userId, role);
    }
}
