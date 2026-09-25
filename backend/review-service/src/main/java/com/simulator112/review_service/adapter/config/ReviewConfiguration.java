package com.simulator112.review_service.adapter.config;

import com.simulator112.review_service.domain.evaluation.DdsReviewRubric;
import com.simulator112.review_service.domain.evaluation.ReviewRubric;
import com.simulator112.review_service.domain.evaluation.System112ReviewRubric;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ReviewConfiguration {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    ReviewRubric system112ReviewRubric() {
        return new System112ReviewRubric();
    }

    @Bean
    ReviewRubric ddsReviewRubric() {
        return new DdsReviewRubric();
    }
}
