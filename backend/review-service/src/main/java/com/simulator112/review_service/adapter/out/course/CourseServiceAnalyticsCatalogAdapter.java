package com.simulator112.review_service.adapter.out.course;

import com.simulator112.review_service.application.port.out.AnalyticsCatalogPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CourseServiceAnalyticsCatalogAdapter implements AnalyticsCatalogPort {
    private final RestClient restClient;

    public CourseServiceAnalyticsCatalogAdapter(RestClient.Builder restClientBuilder,
                                                 @Value("${review.course-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Catalog getCatalog(UUID requesterId, String role) {
        Catalog catalog = restClient.get()
                .uri("/api/v1/course-analytics/catalog")
                .header("X-User-Id", requesterId.toString())
                .header("X-User-Role", role)
                .retrieve()
                .body(Catalog.class);
        if (catalog == null) {
            throw new IllegalStateException("Course-service вернул пустой каталог аналитики");
        }
        return catalog;
    }
}
