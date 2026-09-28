package com.simulator112.incident.application.service;

import com.simulator112.incident.application.service.generation.dds.DdsValidationHarness;

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
    void answersSimulatorRequestWithoutChangingIncidentOrLoadingClassifier() {
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var generator = new IncidentDraftGenerationService(classifier, messages -> {
            if (calls.incrementAndGet() == 1) {
                assertThat(messages.getFirst().content()).contains("Определи намерение").doesNotContain("Пожар в квартире");
                return "{\"intent\":\"ANSWER\"}";
            }
            assertThat(messages.getFirst().content()).contains("Краткая справка", "Пожар в квартире", "Система-112 и ДДС — разные режимы обучения")
                    .doesNotContain("симулятора 112");
            assertThat(messages.get(1).content()).isEqualTo("Это учебный сценарий");
            return "{\"message\":\"В сценарии ДДС задаются этапы реагирования.\"}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Это учебный сценарий"),
                        new GenerateIncidentDraftUseCase.Message("assistant", "Понял"),
                        new GenerateIncidentDraftUseCase.Message("user", "Расскажи про этапы ДДС")),
                mapper.readTree("{\"targetType\":\"DDS\",\"title\":\"Пожар в квартире\",\"stages\":[]}")));

        assertThat(result.message()).isEqualTo("В сценарии ДДС задаются этапы реагирования.");
        assertThat(result.incident().isObject()).isTrue();
        assertThat(result.incident().isEmpty()).isTrue();
        assertThat(calls.get()).isEqualTo(2);
        org.mockito.Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void questionFormCanStillRequestIncidentEdit() {
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = new IncidentDraftGenerationService(classifier, messages -> {
            var prompt = messages.getFirst().content();
            if (prompt.contains("Определи намерение")) {
                assertThat(prompt).contains("Приоритет", "если деталей пока мало");
                return "{\"intent\":\"INCIDENT\"}";
            }
            if (prompt.contains("Выбери ТОЛЬКО поля")) return "{\"paths\":[\"/difficulty\"]}";
            return "{\"message\":\"Сложность изменена\",\"incident\":{\"difficulty\":\"HARD\"}}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Можешь изменить сложность?")),
                mapper.readTree("{\"targetType\":\"SYSTEM_112\",\"title\":\"Пожар\",\"difficulty\":\"NORMAL\",\"stages\":[]}")));

        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
    }

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
    void generatesFresh112ThroughSharedStepsIncludingCriteria() {
        var scenarioCalls = new java.util.concurrent.atomic.AtomicInteger();
        var planCalls = new java.util.concurrent.atomic.AtomicInteger();
        var secondStageCalls = new java.util.concurrent.atomic.AtomicInteger();
        var criteriaCalls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            var prompt = messages.getFirst().content();
            if (prompt.contains("СУТЬ нового учебного сценария Системы-112")) {
                scenarioCalls.incrementAndGet();
                return "{\"scenario\":\"Анна увидела дым в квартире и вызвала 112. Позже обнаружила человека в комнате.\"}";
            }
            assertThat(prompt).contains("Анна увидела дым");
            if (prompt.contains("Запланируй промежуточные этапы")) {
                if (planCalls.incrementAndGet() == 1) return "{\"stagePlan\":[]}";
                return "{\"stagePlan\":[{\"goal\":\"Первый вызов\"},{\"goal\":\"Повторное сообщение\"}]}";
            }
            if (prompt.contains("основные данные нового сценария"))
                return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"NORMAL\"}}";
            if (prompt.contains("Создай только ОДИН этап")) {
                if (prompt.contains("Текущий этап 1 из 2")) return """
                        {"stage":{"title":"Первое сообщение","classifierCodes":["1050101"],"victimCount":0,
                        "calls":[{"person":{"firstName":"Анна","lastName":"Смирнова","phone":"123"},
                        "knownFacts":["Дым в квартире"],"hiddenFacts":[]}]}}
                        """;
                assertThat(prompt).contains("Дым в квартире");
                if (secondStageCalls.incrementAndGet() == 1) return "{\"stage\":{\"title\":\"Второй вызов\",\"calls\":[]}}";
                return """
                        {"stage":{"title":"Второе сообщение","classifierCodes":["1050101"],"victimCount":1,
                        "calls":[{"person":{"firstName":"Анна","lastName":"Смирнова","phone":"123"},
                        "knownFacts":["В комнате обнаружен человек"],"hiddenFacts":[]}]}}
                        """;
            }
            assertThat(prompt).contains("Создай критерии оценки", "Дым в квартире");
            if (criteriaCalls.incrementAndGet() == 1) return """
                    {"message":"Готово","incident":{"dialogueCriteria":[
                    {"name":"Адрес","hypothesis":"Оператор уточнил адрес","weight":30},
                    {"name":"Люди","hypothesis":"Оператор уточнил пострадавших","weight":30}]}}
                    """;
            return """
                    {"message":"Готово","incident":{"dialogueCriteria":[
                    {"name":"Адрес","hypothesis":"Оператор уточнил адрес","weight":20},
                    {"name":"Люди","hypothesis":"Оператор уточнил пострадавших","weight":20}]}}
                    """;
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар с двумя этапами")),
                mapper.readTree("""
                        {"targetType":"SYSTEM_112","title":"","difficulty":"NORMAL","address":{"city":"","floor":0},
                        "stages":[],"dialogueCriteria":[]}
                        """)));
        assertThat(scenarioCalls.get()).isEqualTo(1);
        assertThat(planCalls.get()).isEqualTo(2);
        assertThat(secondStageCalls.get()).isEqualTo(2);
        assertThat(criteriaCalls.get()).isEqualTo(2);
        assertThat(result.incident().path("stages").size()).isEqualTo(2);
        assertThat(result.incident().path("dialogueCriteria").size()).isEqualTo(2);
    }

    @Test
    void populated112DraftStillUsesPatch() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            calls.incrementAndGet();
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/difficulty\"]}";
            return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"HARD\"}}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Измени сложность")),
                mapper.readTree("{\"targetType\":\"SYSTEM_112\",\"title\":\"Пожар\",\"stages\":[]}")));
        assertThat(calls.get()).isEqualTo(2);
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
    }

    @Test
    void acceptsCompleteDdsScenario() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],"victimCount":1,"assignedServices":["MCHS","POLICE"]},
                "initialAssignment":{"emergencyService":"MCHS"},"stages":[
                {"title":"Получение карточки","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]},
                {"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"actualStatus":"ARRIVED",
                 "expectedComment":"Бригада прибыла","calls":[{"direction":"INBOUND","counterparty":"SERVICE",
                 "serviceCode":"POLICE","person":{"firstName":"Иван","lastName":"Петров","phone":"123"},
                 "knownFacts":["Бригада на месте"]}]},
                {"title":"Завершение","type":"COMPLETE_INCIDENT","timeLimitSeconds":30,"calls":[]}]}}
                """);
        assertThat(result.incident().path("preparedCardTemplate").path("assignedServices").size()).isEqualTo(2);
        assertThat(result.incident().path("stages").size()).isEqualTo(3);
        assertThat(result.incident().path("stages").get(1).path("calls").get(0).path("serviceCode").asText()).isEqualTo("POLICE");
    }

    @Test
    void ddsRepairsInvalidModelResponse() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = new ClassifierCatalogPort() {
            @Override public void requireEntry(String code) {}
            @Override public void requireService(String code) {}
            @Override public List<String> resolveAssignedServices(List<String> codes) { return List.of("MCHS", "POLICE"); }
            @Override public List<Candidate> search(String query, int limit, List<String> codes) {
                assertThat(limit).isEqualTo(40);
                return List.of(new Candidate("1050101", "Пожары", "Пожар"));
            }
        };
        var generator = legacyGenerator(classifier, messages -> {
            if (calls.incrementAndGet() == 1) return "{\"paths\":[]}";
            if (calls.get() == 2) {
                assertThat(messages.getLast().content()).contains("предыдущий ответ не прошёл проверку");
                return "{\"paths\":[\"/difficulty\"]}";
            }
            return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"HARD\"}}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Сделай сценарий ДДС")),
                mapper.readTree("{\"targetType\":\"DDS\",\"title\":\"Пожар\"}")));
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void ddsStopsAfterThreeInvalidResponses() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            calls.incrementAndGet();
            return "{\"message\":\"Готово\",\"incident\":{\"stages\":[]}}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        assertThatThrownBy(() -> generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Сделай сценарий ДДС")),
                mapper.readTree("{\"targetType\":\"DDS\",\"title\":\"Пожар\"}"))))
                .isInstanceOf(IncidentGenerationException.class);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void createsFreshDdsScenarioInStepsAndRetriesOnlyFailedStage() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = new ClassifierCatalogPort() {
            @Override public void requireEntry(String code) {}
            @Override public void requireService(String code) {}
            @Override public List<String> resolveAssignedServices(List<String> codes) { return List.of("MCHS"); }
            @Override public List<Candidate> search(String query, int limit, List<String> codes) {
                return List.of(new Candidate("1050101", "Пожары", "Пожар"));
            }
        };
        var generator = legacyGenerator(classifier, messages -> {
            var prompt = messages.getFirst().content();
            calls.incrementAndGet();
            if (prompt.contains("Опиши суть НОВОГО")) return """
                    {"scenario":"Пожар в квартире без пострадавших, бригада выехала и затем прибыла"}
                    """;
            if (prompt.contains("план промежуточных этапов")) return """
                    {"stagePlan":[{"type":"CALL_BRIGADE_FOR_STATUS","goal":"Уточнить выезд"},
                    {"type":"WAIT_FOR_BRIGADE_STATUS_CHANGE","goal":"Дождаться прибытия"}]}
                    """;
            if (prompt.contains("ОСНОВНЫЕ ДАННЫЕ")) return """
                    {"message":"Готово","incident":{"title":"Пожар в квартире","difficulty":"NORMAL"}}
                    """;
            if (prompt.contains("подготовленную карточку")) return """
                    {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                    "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Иванова","phone":"123"}}}}
                    """;
            if (prompt.contains("Текущий этап (1 из 2)")) return """
                    {"stage":{"title":"Выезд","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,
                    "calls":[{"direction":"OUTBOUND","counterparty":"BRIGADE",
                    "person":{"firstName":"Иван","lastName":"Петров","phone":"123"},
                    "knownFacts":["Бригада выехала"],"hiddenFacts":[]}]}}
                    """;
            assertThat(prompt).contains("Бригада выехала", "Дождаться прибытия");
            if (messages.size() < 2) return """
                    {"stage":{"title":"Прибытие","type":"COMPLETE_INCIDENT","timeLimitSeconds":60,"calls":[]}}
                    """;
            if (messages.size() < 3) return "{\"stage\":{\"type\":\"WAIT_FOR_BRIGADE_STATUS_CHANGE\"}}";
            return """
                    {"stage":{"title":"Прибытие","type":"WAIT_FOR_BRIGADE_STATUS_CHANGE",
                    "timeLimitSeconds":60,"calls":[]}}
                    """;
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар с двумя этапами")),
                mapper.readTree("""
                        {"targetType":"DDS","title":"","difficulty":"NORMAL","address":{"city":"","floor":0},
                        "preparedCardTemplate":{"classifierCodes":[""],"victimCount":0},
                        "initialAssignment":{"emergencyService":"MCHS"},
                        "availableServices":[{"code":"MCHS","name":"МЧС"}],"stages":[]}
                        """)));
        assertThat(calls.get()).isEqualTo(8); // scenario, plan, metadata, card, two stages (second retried twice)
        assertThat(result.incident().path("title").asText()).isEqualTo("Пожар в квартире");
        assertThat(result.incident().path("preparedCardTemplate").path("applicant").path("firstName").asText())
                .isEqualTo("Анна");
        assertThat(result.incident().path("preparedCardTemplate").path("assignedServices").get(0).asText()).isEqualTo("MCHS");
        var stages = result.incident().path("stages");
        assertThat(stages.size()).isEqualTo(4);
        assertThat(stages.get(0).path("type").asText()).isEqualTo("ASSIGN_BRIGADE");
        assertThat(stages.get(1).path("calls").get(0).path("knownFacts").get(0).asText()).isEqualTo("Бригада выехала");
        assertThat(stages.get(2).path("type").asText()).isEqualTo("WAIT_FOR_BRIGADE_STATUS_CHANGE");
        assertThat(stages.get(3).path("type").asText()).isEqualTo("COMPLETE_INCIDENT");
    }

    @Test
    void generatesCommonPlanOnceAndRetriesEachFieldBlockUntilValid() {
        var scenarioCalls = new java.util.concurrent.atomic.AtomicInteger();
        var planCalls = new java.util.concurrent.atomic.AtomicInteger();
        var metadataCalls = new java.util.concurrent.atomic.AtomicInteger();
        var cardCalls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        org.mockito.Mockito.when(classifier.resolveAssignedServices(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of("MCHS"));
        var generator = legacyGenerator(classifier, messages -> {
            var prompt = messages.getFirst().content();
            if (prompt.contains("Опиши суть НОВОГО")) {
                scenarioCalls.incrementAndGet();
                return "{\"scenario\":\"Анна сообщила о пожаре в квартире. Бригада уточняет статус выезда.\"}";
            }
            assertThat(prompt).contains("Анна сообщила о пожаре");
            if (prompt.contains("план промежуточных этапов")) {
                planCalls.incrementAndGet();
                return "{\"stagePlan\":[{\"type\":\"CALL_BRIGADE_FOR_STATUS\",\"goal\":\"Уточнить выезд\"}]}";
            }
            if (prompt.contains("ОСНОВНЫЕ ДАННЫЕ")) {
                if (metadataCalls.incrementAndGet() == 1) return "{\"message\":\"Готово\",\"incident\":{\"difficulty\":\"NORMAL\"}}";
                return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"NORMAL\"}}";
            }
            if (prompt.contains("подготовленную карточку")) {
                int attempt = cardCalls.incrementAndGet();
                if (attempt == 1) return """
                        {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                        "victimCount":0}}}
                        """;
                if (attempt == 2) return """
                        {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                        "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Иванова","phone":" "}}}}
                        """;
                return """
                        {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                        "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Иванова","phone":"123"}}}}
                        """;
            }
            assertThat(prompt).contains("Анна сообщила о пожаре", "\"firstName\":\"Анна\"");
            return """
                    {"stage":{"title":"Звонок","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,
                    "calls":[{"direction":"OUTBOUND","counterparty":"BRIGADE"}]}}
                    """;
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай сценарий пожара")),
                mapper.readTree("{\"targetType\":\"DDS\"}")));
        assertThat(scenarioCalls.get()).isEqualTo(1);
        assertThat(planCalls.get()).isEqualTo(1);
        assertThat(metadataCalls.get()).isEqualTo(2);
        assertThat(cardCalls.get()).isEqualTo(3);
        assertThat(result.incident().path("preparedCardTemplate").path("applicant").path("phone").asText()).isEqualTo("123");
    }

    @Test
    void doesNotRetryInvalidScenarioDescription() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            calls.incrementAndGet();
            return "{\"scenario\":\"\"}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        assertThatThrownBy(() -> generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар")),
                mapper.readTree("{\"targetType\":\"DDS\"}"))))
                .isInstanceOf(IncidentGenerationException.class)
                .hasMessageContaining("Описание сценария");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesStagePlanThreeTimesWithoutRegeneratingScenario() {
        var scenarioCalls = new java.util.concurrent.atomic.AtomicInteger();
        var planCalls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            if (messages.getFirst().content().contains("Опиши суть НОВОГО")) {
                scenarioCalls.incrementAndGet();
                return "{\"scenario\":\"Пожар в квартире\"}";
            }
            assertThat(messages.getFirst().content()).contains("план промежуточных этапов");
            planCalls.incrementAndGet();
            return "{\"stagePlan\":[]}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        assertThatThrownBy(() -> generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар")),
                mapper.readTree("{\"targetType\":\"DDS\"}"))))
                .isInstanceOf(IncidentGenerationException.class)
                .cause().hasMessageContaining("stagePlan");
        assertThat(scenarioCalls.get()).isEqualTo(1);
        assertThat(planCalls.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryStageGenerationWhenOllamaIsUnavailable() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            calls.incrementAndGet();
            var prompt = messages.getFirst().content();
            if (prompt.contains("Опиши суть НОВОГО")) return "{\"scenario\":\"Пожар в квартире, бригада выехала\"}";
            if (prompt.contains("план промежуточных этапов")) return """
                    {"stagePlan":[{"type":"CALL_BRIGADE_FOR_STATUS","goal":"Связь"}]}
                    """;
            if (prompt.contains("ОСНОВНЫЕ ДАННЫЕ")) return """
                    {"message":"Готово","incident":{"title":"Пожар","difficulty":"NORMAL"}}
                    """;
            if (prompt.contains("подготовленную карточку")) return """
                    {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                    "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Иванова","phone":"123"}}}}
                    """;
            throw new OllamaUnavailableException("Нет соединения с Ollama", new java.net.ConnectException());
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        assertThatThrownBy(() -> generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар")),
                mapper.readTree("{\"targetType\":\"DDS\"}"))))
                .isInstanceOf(OllamaUnavailableException.class);
        assertThat(calls.get()).isEqualTo(5); // scenario + plan + metadata + card + first stage attempt
    }

    @Test
    void acceptsPlanNestedByModelInsideIncident() {
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages ->
                messages.getFirst().content().contains("Опиши суть НОВОГО") ? """
                        {"scenario":"Пожар в квартире, бригада выехала"}
                        """ : messages.getFirst().content().contains("план промежуточных этапов") ? """
                        {"incident":{"stagePlan":[{"type":"CALL_BRIGADE_FOR_STATUS","goal":"Уточнить выезд"}]}}
                        """ : messages.getFirst().content().contains("ОСНОВНЫЕ ДАННЫЕ") ? """
                        {"message":"Готово","incident":{"title":"Пожар","difficulty":"NORMAL"}}
                        """ : messages.getFirst().content().contains("подготовленную карточку") ? """
                        {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                        "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Иванова","phone":"123"}}}}
                        """ : """
                        {"stage":{"title":"Звонок","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,
                        "calls":[{"direction":"OUTBOUND","counterparty":"BRIGADE"}]}}
                        """, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар")),
                mapper.readTree("{\"targetType\":\"DDS\"}")));
        assertThat(result.incident().has("stagePlan")).isFalse();
        assertThat(result.incident().path("stages").size()).isEqualTo(3);
    }

    @Test
    void populatedDdsDraftUsesPatchInsteadOfStagedGeneration() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            calls.incrementAndGet();
            assertThat(messages.getFirst().content()).doesNotContain("Опиши суть НОВОГО");
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/difficulty\"]}";
            return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"HARD\"}}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Измени сложность")),
                mapper.readTree("""
                        {"targetType":"DDS","title":"","preparedCardTemplate":{"classifierCodes":["1050101"]},
                        "stages":[{"type":"ASSIGN_BRIGADE","calls":[]}]}
                        """)));
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void stopsAfterThreeFailuresForOneIntermediateStage() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new ClassifierCatalogPort.Candidate("1050101", "Пожары", "Пожар")));
        var generator = legacyGenerator(classifier, messages -> {
            calls.incrementAndGet();
            var prompt = messages.getFirst().content();
            if (prompt.contains("Опиши суть НОВОГО")) return "{\"scenario\":\"Пожар в квартире, бригада выехала\"}";
            if (prompt.contains("план промежуточных этапов")) return """
                    {"stagePlan":[{"type":"CALL_BRIGADE_FOR_STATUS","goal":"Связь"}]}
                    """;
            if (prompt.contains("ОСНОВНЫЕ ДАННЫЕ")) return """
                    {"message":"Готово","incident":{"title":"Пожар","difficulty":"NORMAL"}}
                    """;
            if (prompt.contains("подготовленную карточку")) return """
                    {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                    "victimCount":0,"applicant":{"firstName":"Анна","lastName":"Иванова","phone":"123"}}}}
                    """;
            return "{\"stage\":{\"type\":\"COMPLETE_INCIDENT\"}}";
        }, new GeneratedIncidentPatchValidator(mapper), mapper);
        assertThatThrownBy(() -> generator.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай пожар")),
                mapper.readTree("{\"targetType\":\"DDS\",\"stages\":[]}"))))
                .isInstanceOf(IncidentGenerationException.class)
                .cause().hasMessageContaining("Stage type differs from plan");
        assertThat(calls.get()).isEqualTo(7); // scenario + plan + metadata + card + three attempts for the stage
    }

    @Test
    void acceptsGeneratedApplicantAndBrigadeContact() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["1050101"],
                "applicant":{"firstName":"Анна","lastName":"Иванова","phone":"+79991112233","age":32,
                "additionalInfo":"Сообщила о дыме"}},"stages":[
                {"title":"Получение карточки","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]},
                {"title":"Связь с бригадой","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[
                {"direction":"OUTBOUND","counterparty":"BRIGADE","person":{
                "firstName":"Иван","lastName":"Петров","phone":"+79994445566"},"gender":"MAN",
                "emotionalState":"CALM","knownFacts":["Бригада выехала","Адрес получен"],"hiddenFacts":[]}]},
                {"title":"Завершение","type":"COMPLETE_INCIDENT","timeLimitSeconds":60,"calls":[]}]}}
                """);
        assertThat(result.incident().path("preparedCardTemplate").path("applicant").path("firstName").asText()).isEqualTo("Анна");
        assertThat(result.incident().path("stages").get(1).path("calls").get(0).path("person").path("lastName").asText())
                .isEqualTo("Петров");
    }

    @Test
    void movesMisplacedCallPropertiesOutOfPerson() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"stages":[
                {"title":"Получение","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]},
                {"title":"Разговор","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[
                {"direction":"OUTBOUND","counterparty":"BRIGADE","gender":"MAN",
                "person":{"firstName":"Иван","lastName":"Петров","phone":"123",
                "gender":"WOMEN","emotionalState":"CALM","aiContext":"Говорит спокойно"}}]},
                {"title":"Завершение","type":"COMPLETE_INCIDENT","timeLimitSeconds":60,"calls":[]}]}}
                """);
        var call = result.incident().path("stages").get(1).path("calls").get(0);
        assertThat(call.path("gender").asText()).isEqualTo("MAN");
        assertThat(call.path("emotionalState").asText()).isEqualTo("CALM");
        assertThat(call.path("aiContext").asText()).isEqualTo("Говорит спокойно");
        assertThat(call.path("person").has("gender")).isFalse();
        assertThat(call.path("person").path("lastName").asText()).isEqualTo("Петров");
    }

    @Test
    void assemblesBoundariesAroundOnlyIntermediateStageAndPreservesExistingIds() {
        var draft = mapper.readTree("""
                {"targetType":"DDS","initialAssignment":{"emergencyService":"MCHS"},"stages":[
                {"id":"first-id","position":0,"title":"Карточка","type":"ASSIGN_BRIGADE",
                 "timeLimitSeconds":30,"calls":[]},
                {"id":"middle-id","position":1,"title":"Старый контроль","type":"CALL_BRIGADE_FOR_STATUS",
                 "timeLimitSeconds":60,"calls":[]},
                {"id":"last-id","position":2,"title":"Моё завершение","type":"COMPLETE_INCIDENT",
                 "timeLimitSeconds":45,"actualStatus":"COMPLETED","calls":[]}]}
                """);
        var result = DdsValidationHarness.validate("""
                {"message":"Готово","incident":{"stages":[
                  {"id":"middle-id","title":"Новый контроль","type":"CALL_BRIGADE_FOR_STATUS",
                  "timeLimitSeconds":90,"calls":[]}]}}
                """, draft, mapper);
        var stages = result.incident().path("stages");
        assertThat(stages.size()).isEqualTo(3);
        assertThat(stages.get(0).path("id").asText()).isEqualTo("first-id");
        assertThat(stages.get(1).path("id").asText()).isEqualTo("middle-id");
        assertThat(stages.get(1).path("timeLimitSeconds").asInt()).isEqualTo(90);
        assertThat(stages.get(2).path("id").asText()).isEqualTo("last-id");
        assertThat(stages.get(2).path("title").asText()).isEqualTo("Моё завершение");
        assertThat(stages.get(2).path("actualStatus").asText()).isEqualTo("COMPLETED");
        assertThat(stages.get(2).has("position")).isFalse();
    }

    @Test
    void rejectsBoundaryOnlyResponseForNewScenario() {
        assertThatThrownBy(() -> generateDdsResponse("""
                {"message":"Готово","incident":{"stages":[
                  {"title":"Получение","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]},
                  {"title":"Конец","type":"COMPLETE_INCIDENT","timeLimitSeconds":60,"calls":[]}]}}
                """))
                .isInstanceOf(IncidentGenerationException.class)
                .cause().hasMessageContaining("intermediate stage");
    }

    @Test
    void acceptsDdsPatchWithoutReplacingStages() {
        var result = generateDdsResponse("""
                {"message":"Сложность изменена","incident":{"difficulty":"HARD"}}
                """);
        assertThat(result.incident().size()).isEqualTo(1);
    }

    @Test
    void ignoresChangeToAssignedServicesOnly() {
        var result = generateDdsResponse("""
                {"message":"Добавлена полиция","incident":{"preparedCardTemplate":{"assignedServices":["MCHS","POLICE"]}}}
                """);
        assertThat(result.incident().isEmpty()).isTrue();
    }

    @Test
    void ignoresModelAssignedServicesWithoutChangingClassifierCodes() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"preparedCardTemplate":{"assignedServices":["UNKNOWN","UNKNOWN"]}}}
                """);
        assertThat(result.incident().isEmpty()).isTrue();
    }

    @Test
    void resolvesAssignedServicesWhenClassifierCodesChange() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"preparedCardTemplate":{
                  "classifierCodes":["1050101"],"assignedServices":["UNKNOWN","UNKNOWN"]},
                  "initialAssignment":{"emergencyService":"POLICE"}}}
                """);
        assertThat(result.incident().path("preparedCardTemplate").path("assignedServices").get(0).asText()).isEqualTo("MCHS");
        assertThat(result.incident().path("preparedCardTemplate").path("assignedServices").get(1).asText()).isEqualTo("POLICE");
        assertThat(result.incident().has("initialAssignment")).isFalse();
    }

    @Test
    void acceptsExistingFirstStageWithFormPosition() {
        var result = DdsValidationHarness.validate("""
                {"message":"Готово","incident":{"stages":[
                {"id":"invented-first","position":0,"title":"Получение карточки","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[{"id":"ignored-call","position":0,"direction":"OUTBOUND","counterparty":"BRIGADE"}]},
                {"id":"invented-middle","position":1,"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[{"id":"invented-call","position":0,"direction":"OUTBOUND","counterparty":"BRIGADE"}]},
                {"id":"invented-last","position":2,"title":"Завершение","type":"COMPLETE_INCIDENT","timeLimitSeconds":60,"calls":[]}]}}
                """, mapper.readTree("""
                {"targetType":"DDS","title":"Пожар","stages":[{"id":"existing-stage","position":0,
                "title":"Получение карточки","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]}]}
                """), mapper);
        assertThat(result.incident().path("stages").get(0).path("id").asText()).isEqualTo("existing-stage");
        assertThat(result.incident().path("stages").get(0).has("position")).isFalse();
        assertThat(result.incident().path("stages").get(0).path("calls").isEmpty()).isTrue();
        assertThat(result.incident().path("stages").get(1).has("position")).isFalse();
        assertThat(result.incident().path("stages").get(1).has("id")).isFalse();
        assertThat(result.incident().path("stages").get(1).path("calls").get(0).has("position")).isFalse();
        assertThat(result.incident().path("stages").get(1).path("calls").get(0).has("id")).isFalse();
    }

    @Test
    void identifiesInvalidApplicantField() {
        assertThatThrownBy(() -> generateDdsResponse("""
                {"message":"Готово","incident":{"preparedCardTemplate":{
                "applicant":{"firstName":"Иван","additionalInfo":{"injuries":"ожог"}}}}}
                """))
                .isInstanceOf(IncidentGenerationException.class)
                .cause().hasMessageContaining("Invalid person field: additionalInfo");
    }

    @Test
    void identifiesUnexpectedIncidentField() {
        assertThatThrownBy(() -> generateDdsResponse("""
                {"message":"Готово","incident":{"applicant":{"firstName":"Иван"}}}
                """))
                .isInstanceOf(IncidentGenerationException.class)
                .cause().hasMessageContaining("Unknown incident fields: [applicant]");
    }

    @Test
    void rejectsUnexpectedDdsStageFieldWithItsName() {
        assertThatThrownBy(() -> generateDdsResponse("""
                {"message":"Готово","incident":{"stages":[
                {"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[],"unexpected":"value"}]}}
                """))
                .isInstanceOf(IncidentGenerationException.class)
                .cause().hasMessageContaining("unexpected");
    }

    @Test
    void ignoresWrongFirstStageDurationFromModel() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"stages":[
                {"title":"Получение","type":"ASSIGN_BRIGADE","timeLimitSeconds":60,"calls":[]},
                {"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[]},
                {"title":"Конец","type":"COMPLETE_INCIDENT","timeLimitSeconds":30,"calls":[]}]}}
                """);
        assertThat(result.incident().path("stages").get(0).path("timeLimitSeconds").asInt()).isEqualTo(30);
        assertThat(result.incident().path("stages").get(2).path("title").asText()).isEqualTo("Завершение реагирования");
    }

    @Test
    void discardsInventedStageIdentifier() {
        var result = generateDdsResponse("""
                {"message":"Готово","incident":{"stages":[
                {"id":"made-up","title":"Получение","type":"ASSIGN_BRIGADE","timeLimitSeconds":30,"calls":[]},
                {"id":"made-up-middle","title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[]},
                {"id":"new-last","title":"Конец","type":"COMPLETE_INCIDENT","timeLimitSeconds":30,"calls":[]}]}}
                """);
        assertThat(result.incident().path("stages").get(0).has("id")).isFalse();
        assertThat(result.incident().path("stages").get(1).has("id")).isFalse();
        assertThat(result.incident().path("stages").get(2).has("id")).isFalse();
    }

    @Test
    void identifiesInvalidDdsCallFieldWithoutLoggingItsContent() {
        for (var invalid : List.of(
                "\"direction\":\"CALLER\",\"counterparty\":\"BRIGADE\"",
                "\"direction\":\"INBOUND\",\"counterparty\":\"CALLER\"",
                "\"direction\":\"INBOUND\",\"counterparty\":\"BRIGADE\",\"knownFacts\":null",
                "\"direction\":\"INBOUND\",\"counterparty\":\"BRIGADE\",\"gender\":\"MALE\"")) {
            var field = invalid.contains("MALE") ? "gender" : invalid.contains("knownFacts") ? "knownFacts"
                    : invalid.startsWith("\"direction\":\"CALLER\"") ? "direction" : "counterparty";
            assertThatThrownBy(() -> generateDdsResponse("""
                    {"message":"Готово","incident":{"stages":[
                    {"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[{%s}]}]}}
                    """.formatted(invalid)))
                    .isInstanceOf(IncidentGenerationException.class)
                    .cause().hasMessageContaining("DDS stage 2 call 1: " + field);
        }
    }

    @Test
    void rejectsDdsCallToAssignedService() {
        assertThatThrownBy(() -> generateDdsResponse("""
                {"message":"Готово","incident":{"stages":[
                {"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS","timeLimitSeconds":60,"calls":[
                {"direction":"INBOUND","counterparty":"SERVICE","serviceCode":"MCHS"}]}]}}
                """)).isInstanceOf(IncidentGenerationException.class);
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

    @Test
    void rejectsMalformedGeneratedFields() {
        for (var patch : List.of(
                "{\"dialogueCriteria\":[{\"weight\":10}]}",
                "{\"dialogueCriteria\":[{\"name\":\"Адрес\",\"hypothesis\":\"Оператор уточнил адрес\",\"weight\":1.5}]}",
                "{\"address\":{\"floor\":2.5}}",
                "{\"stages\":[]}")) {
            assertThatThrownBy(() -> generateWithModelResponse("{\"message\":\"Готово\",\"incident\":" + patch + "}"))
                    .isInstanceOf(IncidentGenerationException.class);
        }
        for (var field : List.of("\"hiddenFacts\":\"Дым\"", "\"knownFacts\":[{}]", "\"gender\":\"UNKNOWN\"")) {
            assertThatThrownBy(() -> generateWithModelResponse("""
                    {"message":"Готово","incident":{"stages":[{"classifierCodes":["1050101"],
                    "victimCount":0,"calls":[{"person":{"firstName":"Анна","lastName":"Иванова","phone":"123"},
                    "knownFacts":["Дым"],%s}]}]}}
                    """.formatted(field))).isInstanceOf(IncidentGenerationException.class);
        }
    }

    @Test
    void acceptsDifficultyWhenClassifierIsUnavailable() {
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        org.mockito.Mockito.when(classifier.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList()))
                .thenThrow(new IllegalStateException("Unavailable"));
        var service = legacyGenerator(classifier, messages ->
                messages.getFirst().content().contains("Выбери ТОЛЬКО поля")
                        ? "{\"paths\":[\"/difficulty\"]}"
                        : "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"HARD\"}}",
                new GeneratedIncidentPatchValidator(mapper), mapper);
        var result = service.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Измени сложность")), mapper.readTree("{}")));
        assertThat(result.incident().path("difficulty").asText()).isEqualTo("HARD");
    }

    @Test
    void reportsClassifierFailureWhenChangingStages() {
        var classifier = org.mockito.Mockito.mock(ClassifierCatalogPort.class);
        var service = legacyGenerator(classifier,
                messages -> "{\"paths\":[\"/stages\"]}",
                new GeneratedIncidentPatchValidator(mapper), mapper);
        assertThatThrownBy(() -> service.generate(new GenerateIncidentDraftUseCase.Command(
                List.of(new GenerateIncidentDraftUseCase.Message("user", "Создай сценарий")), mapper.readTree("{}"))))
                .isInstanceOf(IncidentGenerationException.class).hasMessageContaining("коды классификатора");
    }

    private GenerateIncidentDraftUseCase.Result generateDdsResponse(String content) {
        return DdsValidationHarness.validate(content, mapper.readTree("""
                {"targetType":"DDS","title":"Пожар","preparedCardTemplate":{"classifierCodes":["1050101"]},
                "initialAssignment":{"emergencyService":"MCHS"},"availableServices":[
                {"code":"MCHS","name":"МЧС"},{"code":"POLICE","name":"Полиция"}],"stages":[]}
                """), mapper);
    }

    private GenerateIncidentDraftUseCase.Result generateWithModelResponse(String content) {
        return new GeneratedIncidentPatchValidator(mapper).validate(content,
                java.util.Set.of("1050101"), java.util.Set.of(), false,
                mapper.readTree("{\"title\":\"Пожар\",\"difficulty\":\"NORMAL\",\"stages\":[],\"dialogueCriteria\":[],\"address\":{}}"));
    }

    private IncidentDraftGenerationService service(String content, AtomicReference<List<String>> searched) {
        var classifier = new ClassifierCatalogPort() {
            @Override public void requireEntry(String code) {}
            @Override public void requireService(String code) {}
            @Override public List<String> resolveAssignedServices(List<String> codes) { return List.of("MCHS", "POLICE"); }
            @Override public List<Candidate> search(String query, int limit, List<String> codes) {
                if (searched != null) searched.set(codes);
                assertThat(query).isNotBlank();
                assertThat(limit).isEqualTo(40);
                return List.of(new Candidate("1050101", "Пожары", "Пожар"));
            }
        };
        IncidentLanguageModelPort model = messages -> {
            assertThat(messages.getFirst().role()).isEqualTo("system");
            if (messages.getFirst().content().contains("Выбери ТОЛЬКО поля"))
                return "{\"paths\":[\"/difficulty\"]}";
            return "{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\",\"difficulty\":\"HARD\"}}";
        };
        return legacyGenerator(classifier, model, new GeneratedIncidentPatchValidator(mapper), mapper);
    }

    private IncidentDraftGenerationService legacyGenerator(ClassifierCatalogPort classifier,
            IncidentLanguageModelPort model, GeneratedIncidentPatchValidator validator, ObjectMapper mapper) {
        return new IncidentDraftGenerationService(classifier, messages ->
                messages.getFirst().content().contains("Определи намерение")
                        ? "{\"intent\":\"INCIDENT\"}" : model.generate(messages), validator, mapper);
    }
}
