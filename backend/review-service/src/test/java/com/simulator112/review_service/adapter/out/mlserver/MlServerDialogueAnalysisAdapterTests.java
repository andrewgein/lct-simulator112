package com.simulator112.review_service.adapter.out.mlserver;

import com.simulator112.review_service.domain.model.ReviewSubmission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MlServerDialogueAnalysisAdapterTests {
    @Test
    void usesMaximumEntailmentScoreAcrossOperatorPhrases() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new MlServerDialogueAnalysisAdapter(
                builder, "http://mlserver", "review-model", 0.8);

        expectInference(server, "Назовите точный адрес", 0.65);
        expectInference(server, "Есть пострадавшие?", 0.91);

        var transcript = List.of(
                new ReviewSubmission.TranscriptPhrase("USER", "Назовите точный адрес"),
                new ReviewSubmission.TranscriptPhrase("ASSISTANT", "Москва"),
                new ReviewSubmission.TranscriptPhrase("USER", "Есть пострадавшие?"));
        var criteria = List.of(new ReviewSubmission.DialogueCriterion(
                "address", "Уточнение адреса", "Оператор уточнил адрес происшествия", 10));

        var result = adapter.analyze(transcript, criteria);

        assertThat(result).singleElement().satisfies(value -> {
            assertThat(value.matched()).isTrue();
            assertThat(value.confidence()).isEqualTo(0.91);
        });
        server.verify();
    }

    @Test
    void doesNotCallMlServerWhenOperatorPhrasesAreAbsent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new MlServerDialogueAnalysisAdapter(
                builder, "http://mlserver", "review-model", 0.8);

        var result = adapter.analyze(
                List.of(new ReviewSubmission.TranscriptPhrase("ASSISTANT", "Москва")),
                List.of(new ReviewSubmission.DialogueCriterion(
                        "address", "Уточнение адреса", "Оператор уточнил адрес происшествия", 10)));

        assertThat(result).singleElement().satisfies(value -> {
            assertThat(value.matched()).isFalse();
            assertThat(value.confidence()).isZero();
        });
        server.verify();
    }

    private void expectInference(MockRestServiceServer server, String phrase, double score) {
        server.expect(once(), requestTo("http://mlserver/v2/models/review-model/infer"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("array_inputs"),
                        containsString("candidate_labels"),
                        containsString(phrase),
                        containsString("Оператор уточнил адрес происшествия"))))
                .andRespond(withSuccess("""
                        {
                          "outputs": [
                            {
                              "name": "output-0",
                              "shape": [1, 1],
                              "datatype": "BYTES",
                              "data": ["Оператор уточнил адрес происшествия"]
                            },
                            {
                              "name": "output-1",
                              "shape": [1, 1],
                              "datatype": "FP64",
                              "data": [%s]
                            }
                          ]
                        }
                        """.formatted(score), MediaType.APPLICATION_JSON));
    }
}
