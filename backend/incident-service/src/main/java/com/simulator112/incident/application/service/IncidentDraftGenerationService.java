package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentDraftGenerationService implements GenerateIncidentDraftUseCase {
    private static final String SYSTEM_PROMPT = """
            Ты помогаешь администратору создавать учебные сценарии для оператора Системы-112 на русском языке.
            Верни ТОЛЬКО JSON-объект: {"message":"короткий ответ пользователю","incident":{...}}.
            В incident укажи ТОЛЬКО поля, которые пользователь попросил изменить. Если нужно только ответить на вопрос, верни incident: {}.
            Изменение сложности: {"message":"Сложность изменена","incident":{"difficulty":"HARD"}}.
            Допустимые поля incident: title (строка), difficulty (EASY|NORMAL|HARD),
            address:{city,street,house,building,apartment,floor},
            stages:[{title,description,classifierCodes:["код из списка"],victimCount,additionalInfo:{"ключ":"строковое значение"},calls:[{
            direction:"INBOUND",counterparty:"CALLER",gender:"MAN" или "WOMEN",
            person:{firstName,lastName,middleName,age,phone,contactPhone,onScenePhone,address,additionalInfo},
            knownFacts:[строки],hiddenFacts:[строки],aiContext:строка,emotionalState:строка}]}],
            dialogueCriteria:[{name,hypothesis,weight}].
            Для address перечисляй только изменяемые части. city, street, house, building, apartment — строки или null; floor — целое число или null.
            Если изменяешь stages или dialogueCriteria, верни ВЕСЬ соответствующий массив,
            сохранив остальные этапы, звонки и критерии из текущего черновика. Не добавляй id: интерфейс восстановит идентификаторы.
            Каждый этап должен иметь валидный код из списка и хотя бы один входящий звонок с именем, фамилией, телефоном и известными фактами.
            Сумма weight критериев не более 40. Не выдумывай коды классификатора и не меняй поля без запроса пользователя.
            """;

    private final ClassifierCatalogPort classifierCatalog;
    private final IncidentLanguageModelPort model;
    private final GeneratedIncidentPatchValidator validator;
    private final ObjectMapper mapper;

    @Override
    public Result generate(Command command) {
        validateRequest(command);
        var includedCodes = new ArrayList<String>();
        for (var stage : command.draft().path("stages")) {
            for (var code : stage.path("classifierCodes")) {
                if (code.isTextual() && !includedCodes.contains(code.asText())) includedCodes.add(code.asText());
            }
        }
        if (includedCodes.size() > 15) throw new IllegalArgumentException("Слишком много кодов в черновике");
        var searchQuery = command.messages().stream().filter(message -> "user".equals(message.role()))
                .map(Message::content).reduce("", (left, right) -> left + " " + right);
        searchQuery += " " + command.draft().path("title").asText("");
        if (searchQuery.length() > 4000) searchQuery = searchQuery.substring(searchQuery.length() - 4000);
        List<ClassifierCatalogPort.Candidate> candidates = List.of();
        Exception classifierFailure = null;
        try {
            candidates = classifierCatalog.search(searchQuery, 80, includedCodes);
        } catch (Exception e) {
            classifierFailure = e;
        }
        var classifierNames = new LinkedHashMap<String, String>();
        for (var candidate : candidates) {
            classifierNames.put(candidate.code(), candidate.categoryName() + ": " + candidate.finalName());
        }
        try {
            var messages = new ArrayList<IncidentLanguageModelPort.Message>();
            messages.add(new IncidentLanguageModelPort.Message("system", SYSTEM_PROMPT + "\nДоступные коды классификатора: "
                    + mapper.writeValueAsString(classifierNames) + "\nТекущий черновик: "
                    + mapper.writeValueAsString(command.draft())));
            for (var message : command.messages()) {
                messages.add(new IncidentLanguageModelPort.Message(message.role(), message.content()));
            }
            var content = model.generate(messages);
            if (mapper.readTree(content).path("incident").has("stages") && candidates.isEmpty()) {
                throw new ClassifierUnavailableException(classifierFailure);
            }
            return validator.validate(content, classifierNames.keySet());
        } catch (IncidentGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось подготовить запрос генерации", e);
        }
    }

    private void validateRequest(Command command) {
        if (command == null || command.messages() == null || command.messages().isEmpty() || command.messages().size() > 20
                || command.draft() == null || !command.draft().isObject()
                || command.messages().stream().anyMatch(m -> m == null || !List.of("user", "assistant").contains(m.role())
                    || m.content() == null || m.content().length() > 4000)
                || !"user".equals(command.messages().getLast().role())) {
            throw new IllegalArgumentException("Некорректный запрос к генератору");
        }
    }
}
