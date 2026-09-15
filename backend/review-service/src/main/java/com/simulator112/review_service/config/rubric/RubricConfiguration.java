package com.simulator112.review_service.config.rubric;

import com.simulator112.review_service.service.Rubric;
import com.simulator112.review_service.service.RubricBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RubricConfiguration {

    @Bean
    public Rubric rubric() {
        return new RubricBuilder()
                .addStage(new FiledCheckStage())
                .build();
    }
}
