package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentDraftGenerationServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsDifficultyOnlyWithoutReplacingExistingScenario() {
        var result = generateWithModelResponse("""
                {"message":"Сложность изменена","incident":{"difficulty":"HARD"}}
                """);
        assertThat(result.message()).isEqualTo("Сложность изменена");
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
        assertThat(result.incident().size()).isEqualTo(1);
    }

    @Test
    void acceptsCompleteIncidentAsPatch() {
        var result = generateWithModelResponse("""
                {"message":"Сценарий создан","incident":{"title":"Пожар","difficulty":"HARD",
                "address":{"city":"Москва","floor":3},"stages":[{"classifierCodes":["1050101"],
                "victimCount":1,"calls":[{"person":{"firstName":"Анна","lastName":"Иванова","phone":"123"},
                "knownFacts":["Дым в квартире"]}]}],"dialogueCriteria":[]}}
                """);
        assertThat(result.incident().path("stages").size()).isEqualTo(1);
    }

    @Test
    void rejectsCodeNotReturnedByClassifierSearch() {
        assertThatThrownBy(() -> generateWithModelResponse("""
                {"message":"Готово","incident":{"stages":[{"classifierCodes":["invalid"],
                "victimCount":0,"calls":[{"person":{"firstName":"Анна","lastName":"Иванова","phone":"123"},
                "knownFacts":["Дым"]}]}]}}
                """))
                .isInstanceOf(IncidentGenerationException.class);
    }

    @Test
    void normalizesAddressNumbersReturnedAsStringsOrNumbers() {
        var address = generateWithModelResponse("""
                {"message":"Адрес изменён","incident":{"address":{"floor":" 3 ","house":12,"apartment":5}}}
                """).incident().path("address");
        assertThat(address.path("floor").asInt()).isEqualTo(3);
        assertThat(address.path("floor").isNumber()).isTrue();
        assertThat(address.path("house").asText()).isEqualTo("12");
        assertThat(address.path("house").isTextual()).isTrue();
        assertThat(address.path("apartment").asText()).isEqualTo("5");
    }

    @Test
    void rejectsInvalidFloorInsteadOfApplyingItToTheForm() {
        assertThatThrownBy(() -> generateWithModelResponse("""
                {"message":"Адрес изменён","incident":{"address":{"floor":"третий"}}}
                """))
                .isInstanceOf(IncidentGenerationException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidPartialDifficulty() {
        assertThatThrownBy(() -> generateWithModelResponse("""
                {"message":"Готово","incident":{"difficulty":"Сложно"}}
                """))
                .isInstanceOf(IncidentGenerationException.class);
    }

    @Test
    void rejectsInvalidInputBeforeCallingCollaborators() {
        var service = service("{}", null);
        assertThatThrownBy(() -> service.generate(new GenerateIncidentDraftUseCase.Command(List.of(), mapper.readTree("{}"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchesByTextAndIncludesCodesFromDraft() {
        var searched = new AtomicReference<List<String>>();
        var service = service("{\"message\":\"Готово\",\"incident\":{}}", searched);
        var request = new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Горит квартира")),
                mapper.readTree("{\"title\":\"Пожар\",\"stages\":[{\"classifierCodes\":[\"1050101\"]}]}"));
        service.generate(request);
        assertThat(searched.get()).containsExactly("1050101");
    }

    private GenerateIncidentDraftUseCase.Result generateWithModelResponse(String content) {
        return service(content, null).generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Поменяй сложность")),
                mapper.readTree("{\"title\":\"Пожар\",\"difficulty\":\"NORMAL\",\"stages\":[],\"dialogueCriteria\":[],\"address\":{}}")));
    }

    private IncidentDraftGenerationService service(String content, AtomicReference<List<String>> searched) {
        var classifier = new ClassifierCatalogPort() {
            @Override public void requireEntry(String code) {}
            @Override public void requireService(String code) {}
            @Override public List<Candidate> search(String query, int limit, List<String> codes) {
                if (searched != null) searched.set(codes);
                assertThat(query).isNotBlank();
                assertThat(limit).isEqualTo(80);
                return List.of(new Candidate("1050101", "Пожары", "Пожар"));
            }
        };
        IncidentLanguageModelPort model = messages -> {
            assertThat(messages.getFirst().role()).isEqualTo("system");
            assertThat(messages.getFirst().content()).contains("1050101");
            return content;
        };
        return new IncidentDraftGenerationService(classifier, model, new GeneratedIncidentPatchValidator(mapper), mapper);
    }
}
