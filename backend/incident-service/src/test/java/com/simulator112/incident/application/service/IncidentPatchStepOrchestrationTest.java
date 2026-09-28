package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IncidentPatchStepOrchestrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ClassifierCatalogPort catalog = mock(ClassifierCatalogPort.class);

    private IncidentDraftGenerationService service(IncidentLanguageModelPort model) {
        when(catalog.search(anyString(), anyInt(), anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        when(catalog.resolveAssignedServices(anyList())).thenReturn(List.of("MCHS"));
        return new IncidentDraftGenerationService(catalog, messages ->
                messages.getFirst().content().contains("Определи намерение")
                        ? "{\"intent\":\"UPDATE\"}" : model.generate(messages),
                new GeneratedIncidentPatchValidator(mapper), mapper);
    }

    private GenerateIncidentDraftUseCase.Command command(String text, String draft) {
        return new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", text)), mapper.readTree(draft));
    }

    @Test
    void editsOnlySelectedBasicFieldsInOneStep() {
        var prompts = new ArrayList<String>();
        var generator = service(messages -> {
            var prompt = messages.getFirst().content();
            prompts.add(prompt);
            if (prompt.contains("Выбери ТОЛЬКО поля")) return "{\"paths\":[\"/difficulty\",\"/address/city\"]}";
            return """
                    {"message":"Готово","incident":{"difficulty":"HARD",
                    "address":{"city":"Москва","street":"Нежелательная улица"}}}
                    """;
        });
        var result = generator.generate(command("Измени сложность и город", """
                {"targetType":"SYSTEM_112","title":"Пожар","difficulty":"NORMAL",
                "address":{"city":"Тула","street":"Ленина"},"stages":[],"dialogueCriteria":[]}
                """));
        assertThat(prompts).hasSize(2);
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
        assertThat(result.incident().path("address").path("city").asText()).isEqualTo("Москва");
        assertThat(result.incident().path("address").has("street")).isFalse();
        assertThat(result.incident().has("title")).isFalse();
    }

    @Test
    void retriesAddressPatchThatOnlyRepeatsExistingCity() {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        var generator = service(messages -> {
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/address\"]}";
            if (attempts.incrementAndGet() == 1)
                return "{\"incident\":{\"address\":{\"city\":\"Москва\"}}}";
            return "{\"incident\":{\"address\":{\"city\":\"Москва\",\"street\":\"Ленина\",\"house\":\"12\"}}}";
        });
        var result = generator.generate(command("Можешь заполнить адрес?", """
                {"targetType":"SYSTEM_112","title":"","difficulty":"NORMAL",
                "address":{"city":"Москва","street":"","house":""},"stages":[],"dialogueCriteria":[]}
                """));
        assertThat(attempts.get()).isEqualTo(2);
        assertThat(result.incident().path("address").path("street").asText()).isEqualTo("Ленина");
        assertThat(result.incident().path("address").path("house").asText()).isEqualTo("12");
        assertThat(result.incident().has("title")).isFalse();
    }

    @Test
    void convertsStructuredApplicantAddressToFormText() {
        var generator = service(messages -> {
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/preparedCardTemplate/applicant\"]}";
            return """
                    {"incident":{"preparedCardTemplate":{"applicant":{"firstName":"Анна",
                    "lastName":"Смирнова","phone":"+79990000001",
                    "address":{"city":"Москва","street":"Ленина","house":12,"apartment":5}}}}}
                    """;
        });
        var result = generator.generate(command("Можешь заполнить заявителя?", """
                {"targetType":"DDS","title":"","preparedCardTemplate":{"classifierCodes":[""],"applicant":null},
                "initialAssignment":{"emergencyService":""},"stages":[]}
                """));
        var applicant = result.incident().path("preparedCardTemplate").path("applicant");
        assertThat(applicant.path("firstName").asText()).isEqualTo("Анна");
        assertThat(applicant.path("address").asText()).isEqualTo("Москва, Ленина, д. 12, кв. 5");
        assertThat(result.incident().has("stages")).isFalse();
    }

    @Test
    void editsOnlyOneCallFieldAndPreservesStageAndCallIds() {
        var generator = service(messages -> {
            var prompt = messages.getFirst().content();
            if (prompt.contains("Выбери ТОЛЬКО поля")) return "{\"paths\":[\"/stages/0/calls/0/person/phone\"]}";
            return """
                    {"stage":{"title":"Новое название","classifierCodes":["1050101"],"victimCount":0,
                    "calls":[{"direction":"INBOUND","counterparty":"CALLER",
                    "person":{"firstName":"Другая","lastName":"Женщина","phone":"+79990000002"},
                    "knownFacts":["Дым"],"hiddenFacts":[]}]}}
                    """;
        });
        var result = generator.generate(command("Измени только телефон заявителя", """
                {"targetType":"SYSTEM_112","title":"Пожар","stages":[{"id":"stage-1","title":"Вызов",
                "classifierCodes":["1050101"],"victimCount":0,"calls":[{"id":"call-1","direction":"INBOUND",
                "counterparty":"CALLER","person":{"firstName":"Анна","lastName":"Смирнова","phone":"+79990000001"},
                "knownFacts":["Пожар в комнате"],"hiddenFacts":[]}]}],"dialogueCriteria":[]}
                """));
        var stage = result.incident().path("stages").get(0);
        assertThat(stage.path("id").asText()).isEqualTo("stage-1");
        assertThat(stage.path("title").asText()).isEqualTo("Вызов");
        assertThat(stage.path("calls").get(0).path("id").asText()).isEqualTo("call-1");
        assertThat(stage.path("calls").get(0).path("person").path("firstName").asText()).isEqualTo("Анна");
        assertThat(stage.path("calls").get(0).path("person").path("phone").asText()).isEqualTo("+79990000002");
    }

    @Test
    void protectsDdsBoundariesAndEditsOnlyIntermediateCall() {
        var generator = service(messages -> {
            var prompt = messages.getFirst().content();
            if (prompt.contains("Выбери ТОЛЬКО поля")) return "{\"paths\":[\"/stages/1/calls/0/person/phone\"]}";
            return """
                    {"stage":{"title":"Другое название","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":120,
                    "calls":[{"direction":"OUTBOUND","counterparty":"BRIGADE",
                    "person":{"firstName":"Другой","lastName":"Сотрудник","phone":"+79990000002"},
                    "knownFacts":["Бригада выехала"],"hiddenFacts":[]}]}}
                    """;
        });
        var result = generator.generate(command("Измени только телефон бригады", """
                {"targetType":"DDS","title":"Пожар","initialAssignment":{"emergencyService":"MCHS"},
                "availableServices":[{"code":"MCHS","name":"МЧС"}],
                "stages":[{"id":"first","title":"Карточка","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]},
                {"id":"middle","title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,
                "calls":[{"id":"call","direction":"OUTBOUND","counterparty":"BRIGADE",
                "person":{"firstName":"Иван","lastName":"Петров","phone":"+79990000001"},
                "knownFacts":["Бригада едет"],"hiddenFacts":[]}]},
                {"id":"last","title":"Завершение","type":"COMPLETE_INCIDENT","timeLimitSeconds":60,"calls":[]}]}
                """));
        var stages = result.incident().path("stages");
        assertThat(stages.size()).isEqualTo(3);
        assertThat(stages.get(0).path("id").asText()).isEqualTo("first");
        assertThat(stages.get(1).path("id").asText()).isEqualTo("middle");
        assertThat(stages.get(1).path("title").asText()).isEqualTo("Контроль");
        assertThat(stages.get(1).path("calls").get(0).path("id").asText()).isEqualTo("call");
        assertThat(stages.get(1).path("calls").get(0).path("person").path("firstName").asText()).isEqualTo("Иван");
        assertThat(stages.get(1).path("calls").get(0).path("person").path("phone").asText()).isEqualTo("+79990000002");
        assertThat(stages.get(2).path("id").asText()).isEqualTo("last");
    }

    @Test
    void ignoresInvalidUnselectedCardFieldsAndAcceptsPartialRegeneration() {
        var generator = service(messages -> {
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/preparedCardTemplate/applicant/phone\"]}";
            return """
                    {"message":"Готово","incident":{"preparedCardTemplate":{
                    "classifierCodes":["invented-code"],"applicant":{"phone":"+79990000002"}}}}
                    """;
        });
        var result = generator.generate(command("Исправь только телефон заявителя", """
                {"targetType":"DDS","title":"Пожар","preparedCardTemplate":{"classifierCodes":["1050101"],
                "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Петрова","phone":"+79990000001"}},
                "initialAssignment":{"emergencyService":"MCHS"},
                "availableServices":[{"code":"MCHS","name":"МЧС"}],"stages":[]}
                """));
        assertThat(result.incident().path("preparedCardTemplate").path("applicant").path("phone").asText())
                .isEqualTo("+79990000002");
        assertThat(result.incident().path("preparedCardTemplate").has("classifierCodes")).isFalse();
    }

    @Test
    void editsApplicantPhoneWithoutReplacingCardCodesOrOtherApplicantFields() {
        var prompts = new ArrayList<String>();
        var generator = service(messages -> {
            var prompt = messages.getFirst().content();
            prompts.add(prompt);
            if (prompt.contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/preparedCardTemplate/applicant/phone\"]}";
            return """
                    {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                    "victimCount":1,"applicant":{"firstName":"Другое имя","lastName":"Иванова",
                    "phone":"+79990000002"}}}}
                    """;
        });
        var result = generator.generate(command("Измени только телефон заявителя", """
                {"targetType":"DDS","title":"Пожар","preparedCardTemplate":{"classifierCodes":["1050101"],
                "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Петрова","phone":"+79990000001"}},
                "initialAssignment":{"emergencyService":"MCHS"},
                "availableServices":[{"code":"MCHS","name":"МЧС"}],"stages":[]}
                """));
        assertThat(prompts).hasSize(2);
        var card = result.incident().path("preparedCardTemplate");
        assertThat(card.has("classifierCodes")).isFalse();
        assertThat(card.has("victimCount")).isFalse();
        assertThat(card.path("applicant").path("firstName").asText()).isEqualTo("Анна");
        assertThat(card.path("applicant").path("phone").asText()).isEqualTo("+79990000002");
        assertThat(result.incident().has("initialAssignment")).isFalse();
    }

    @Test
    void canSelectBasicFieldsAndCriteriaInOnePatch() {
        var prompts = new ArrayList<String>();
        var generator = service(messages -> {
            var prompt = messages.getFirst().content();
            prompts.add(prompt);
            if (prompt.contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/difficulty\",\"/dialogueCriteria/0/weight\"]}";
            if (prompt.contains("основные данные нового сценария"))
                return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Новый заголовок\",\"difficulty\":\"HARD\"}}";
            return """
                    {"message":"Готово","incident":{"dialogueCriteria":[
                    {"name":"Другое название","hypothesis":"Оператор уточнил адрес","weight":15}]}}
                    """;
        });
        var result = generator.generate(command("Измени сложность и вес первого критерия", """
                {"targetType":"SYSTEM_112","title":"Пожар","difficulty":"NORMAL","stages":[],
                "dialogueCriteria":[{"name":"Адрес","hypothesis":"Оператор уточнил адрес","weight":10}]}
                """));
        assertThat(prompts).hasSize(3);
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
        assertThat(result.incident().has("title")).isFalse();
        var criterion = result.incident().path("dialogueCriteria").get(0);
        assertThat(criterion.path("name").asText()).isEqualTo("Адрес");
        assertThat(criterion.path("weight").asInt()).isEqualTo(15);
    }

    @Test
    void replacingCallsRetainsOnlyMatchingExistingCallIds() {
        var generator = service(messages -> {
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/stages/0/calls\"]}";
            return """
                    {"stage":{"title":"Новый заголовок","classifierCodes":["1050101"],"victimCount":0,
                    "calls":[{"id":"invented","direction":"INBOUND","counterparty":"CALLER",
                    "person":{"firstName":"Анна","lastName":"Иванова","phone":"123"},
                    "knownFacts":["Дым"],"hiddenFacts":[]}]}}
                    """;
        });
        var result = generator.generate(command("Обнови звонок", """
                {"targetType":"SYSTEM_112","title":"Пожар","stages":[{"id":"stage-id","title":"Вызов",
                "classifierCodes":["1050101"],"victimCount":0,"calls":[{"id":"call-id","direction":"INBOUND",
                "counterparty":"CALLER","person":{"firstName":"Анна","lastName":"Иванова","phone":"123"},
                "knownFacts":["Пожар"],"hiddenFacts":[]}]}],"dialogueCriteria":[]}
                """));
        var stage = result.incident().path("stages").get(0);
        assertThat(stage.path("id").asText()).isEqualTo("stage-id");
        assertThat(stage.path("title").asText()).isEqualTo("Вызов");
        assertThat(stage.path("calls").get(0).path("id").asText()).isEqualTo("call-id");
    }

    @Test
    void neverLetsSelectorEditDdsBoundary() {
        var generator = service(messages -> "{\"paths\":[\"/stages/0/title\"]}");
        assertThatThrownBy(() -> generator.generate(command("Измени начало", """
                {"targetType":"DDS","title":"Пожар","stages":[
                {"type":"ASSIGN_BRIGADE","title":"Карточка","calls":[]},
                {"type":"COMPLETE_INCIDENT","title":"Конец","calls":[]}]}
                """))).isInstanceOf(IncidentGenerationException.class);
    }
}
