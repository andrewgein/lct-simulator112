package com.simulator112.review_service.adapter.out.llm;

import com.simulator112.review_service.application.port.out.RecommendationPort;
import com.simulator112.review_service.domain.model.ErrorStatistic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class LlmRecommendationAdapter implements RecommendationPort {
    private static final String SYSTEM_PROMPT = """
            Ты — методист тренажёра диспетчеров экстренных служб. По статистике ошибок обучающегося
            составь короткие персональные рекомендации на русском языке. Используй только переданные данные,
            не придумывай факты. Сначала укажи самые частые системные ошибки (высокий errorRate при
            attempts >= 2), затем разовые. Каждая рекомендация — отдельная строка с конкретным советом
            по действию, без общих фраз. Не используй нумерацию и markdown.""";

    private final RestClient restClient;
    private final String model;

    public LlmRecommendationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${review.recommendation.base-url}") String baseUrl,
            @Value("${review.recommendation.api-key:}") String apiKey,
            @Value("${review.recommendation.model}") String model) {
        this.restClient = restClientBuilder.baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    if (apiKey != null && !apiKey.isBlank()) headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
                })
                .build();
        this.model = model;
    }

    @Override
    public List<String> recommend(List<ErrorStatistic> statistics) {
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", SYSTEM_PROMPT),
                                Map.of("role", "user", "content", toPrompt(statistics)))))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
            throw new IllegalStateException("LLM не вернул рекомендации");
        }
        String content = response.get("choices").get(0).path("message").path("content").asText("");
        return Arrays.stream(content.split("\\R"))
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private String toPrompt(List<ErrorStatistic> statistics) {
        StringBuilder builder = new StringBuilder("Статистика по критериям оценки:\n");
        for (ErrorStatistic statistic : statistics) {
            builder.append("- ").append(statistic.criterionName())
                    .append(": провалено ").append(statistic.failedCount()).append(" из ").append(statistic.attempts())
                    .append(" попыток, средний балл ").append(statistic.scoreEarned()).append("/").append(statistic.scoreMax())
                    .append('\n');
        }
        return builder.toString();
    }
}
