package com.simulator112.review_service.adapter.out.mlserver;

import com.simulator112.review_service.application.port.out.DialogueAnalysisPort;
import com.simulator112.review_service.domain.model.ReviewSubmission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Component
public class MlServerDialogueAnalysisAdapter implements DialogueAnalysisPort {
    private static final String OPERATOR = "USER";

    private final RestClient restClient;
    private final String modelName;
    private final double threshold;

    public MlServerDialogueAnalysisAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${review.dialogue-analysis.base-url}") String baseUrl,
            @Value("${review.dialogue-analysis.model-name}") String modelName,
            @Value("${review.dialogue-analysis.threshold:0.8}") double threshold) {
        if (threshold < 0 || threshold > 1) throw new IllegalArgumentException("Порог NLI должен быть от 0 до 1");
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.modelName = modelName;
        this.threshold = threshold;
    }

    @Override
    public List<DialogueAnalysis> analyze(List<ReviewSubmission.TranscriptPhrase> transcript,
                                          List<ReviewSubmission.DialogueCriterion> criteria) {
        if (criteria.isEmpty()) return List.of();
        List<String> operatorPhrases = transcript.stream()
                .filter(phrase -> OPERATOR.equals(phrase.speaker()))
                .map(ReviewSubmission.TranscriptPhrase::text)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        if (operatorPhrases.isEmpty()) {
            return criteria.stream().map(value -> new DialogueAnalysis(value.id(), false, 0)).toList();
        }

        return criteria.stream().map(criterion -> {
            double confidence = operatorPhrases.stream()
                    .mapToDouble(phrase -> infer(phrase, criterion.hypothesis()))
                    .max()
                    .orElse(0);
            return new DialogueAnalysis(criterion.id(), confidence >= threshold, confidence);
        }).toList();
    }

    private double infer(String premise, String hypothesis) {
        Map<String, Object> premiseInput = stringInput("array_inputs", "Оператор: " + premise);
        Map<String, Object> hypothesisInput = stringInput("candidate_labels", hypothesis);

        JsonNode response = restClient.post()
                .uri("/v2/models/{model}/infer", modelName)
                .body(Map.of("inputs", List.of(premiseInput, hypothesisInput)))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("outputs") || response.get("outputs").isEmpty()) {
            throw new IllegalStateException("MLServer не вернул NLI-результаты");
        }

        for (JsonNode output : response.get("outputs")) {
            if ("output-1".equals(output.path("name").asText())) {
                JsonNode data = output.get("data");
                if (data != null && data.isArray() && data.size() == 1 && data.get(0).isNumber()) {
                    double score = data.get(0).asDouble();
                    if (score >= 0 && score <= 1) return score;
                }
            }
        }
        throw new IllegalStateException("Некорректный формат NLI-ответа MLServer");
    }

    private Map<String, Object> stringInput(String name, String value) {
        return Map.of(
                "name", name,
                "shape", List.of(1),
                "datatype", "BYTES",
                "parameters", Map.of("content_type", "str"),
                "data", List.of(value));
    }
}
