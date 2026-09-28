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
            stages:[{title,description,classifierCodes:["код из списка"],victimCount,additionalInfo:{},calls:[{
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

    private static final String DDS_PROMPT = """
            Ты помогаешь администратору создавать учебные сценарии диспетчера ДДС на русском языке.
            Верни ТОЛЬКО JSON: {"message":"короткий ответ","incident":{...}}.
            В incident указывай ТОЛЬКО изменяемые по запросу поля. Если нужен только ответ, верни incident: {}.
            Допустимые поля: title, difficulty (EASY|NORMAL|HARD), address (city,street,house,building,apartment,floor),
            preparedCardTemplate:{classifierCodes:[коды из списка],applicant:{firstName,lastName,middleName,age,phone,contactPhone,onScenePhone,address,additionalInfo},victimCount,additionalInfo:{},assignedServices:[коды служб из списка]},
            initialAssignment:{emergencyService:код службы из списка},
            stages:[{title,description,type,timeLimitSeconds,actualStatus,expectedComment,calls:[{
            direction:INBOUND|OUTBOUND,counterparty:BRIGADE|SERVICE,serviceCode:код другой службы (только для SERVICE),
            person:{firstName,lastName,middleName,age,phone,contactPhone,onScenePhone,address,additionalInfo},
            gender:MAN|WOMEN,knownFacts:[строки],hiddenFacts:[строки],aiContext,emotionalState}]}].
            assignedServices — службы, реагирующие на происшествие; они могут отличаться от службы ДДС
            в initialAssignment.emergencyService. При изменении типов происшествия обнови assignedServices,
            если пользователь просит изменить состав реагирующих служб. В preparedCardTemplate и initialAssignment
            возвращай только изменяемые поля. Если меняешь stages,
            верни ВЕСЬ массив этапов со всеми их звонками. Сохраняй id существующих этапов и звонков
            из текущего черновика при изменении или перестановке. Новым этапам и звонкам id не добавляй.
            Первый этап: ASSIGN_BRIGADE (получение карточки), последний: COMPLETE_INCIDENT (завершение реагирования).
            Остальные типы: WAIT_FOR_BRIGADE_STATUS_CHANGE, CALL_BRIGADE_FOR_STATUS, REQUEST_ADDITIONAL_SERVICE.
            Первый этап длится ровно 30 секунд; остальные этапы — положительное целое число секунд.
            Этапы переключаются по времени, не по действиям диспетчера.
            actualStatus — фактический статус после этапа, один из: ADDED, RECEIVED_BY_SERVICE, ACCEPTED,
            NOT_ACCEPTED, RESPONSE_STARTED, ARRIVED, WORK_IN_PROGRESS, WORK_COMPLETED, WORK_REFUSED,
            REGISTERED, PROCESSED, VERIFIED, NOT_NOTIFIED, REFUSED, NOT_COMPLETED, COMPLETED; либо null.
            expectedComment — ожидаемый смысл комментария (если заполнен, на этапе должен быть звонок).
            SERVICE-звонок требует serviceCode другой службы, отличной от initialAssignment.emergencyService.
            Для address перечисляй только изменяемые части; floor — неотрицательное целое или null.
            Не выдумывай коды классификатора и служб. Не меняй поля без запроса пользователя.
            """;

    private final ClassifierCatalogPort classifierCatalog;
    private final IncidentLanguageModelPort model;
    private final GeneratedIncidentPatchValidator validator;
    private final ObjectMapper mapper;

    @Override
    public Result generate(Command command) {
        validateRequest(command);
        var includedCodes = new ArrayList<String>();
        boolean dds = "DDS".equals(command.draft().path("targetType").asText());
        if (dds) for (var code : command.draft().path("preparedCardTemplate").path("classifierCodes")) {
            if (code.isTextual() && !code.asText().isBlank() && !includedCodes.contains(code.asText())) includedCodes.add(code.asText());
        }
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
        List<ClassifierCatalogPort.Candidate> candidates;
        try {
            candidates = classifierCatalog.search(searchQuery, 80, includedCodes);
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось получить коды классификатора", e);
        }
        if (candidates.isEmpty()) throw new IncidentGenerationException("No classifier candidates");
        var classifierNames = new LinkedHashMap<String, String>();
        for (var candidate : candidates) {
            classifierNames.put(candidate.code(), candidate.categoryName() + ": " + candidate.finalName());
        }
        try {
            var services = new LinkedHashMap<String, String>();
            if (dds) for (var service : command.draft().path("availableServices")) {
                if (service.path("code").isTextual() && service.path("name").isTextual() && services.size() < 100) {
                    services.put(service.path("code").asText(), service.path("name").asText());
                }
            }
            var messages = new ArrayList<IncidentLanguageModelPort.Message>();
            messages.add(new IncidentLanguageModelPort.Message("system", (dds ? DDS_PROMPT : SYSTEM_PROMPT)
                    + "\nДоступные коды классификатора: " + mapper.writeValueAsString(classifierNames)
                    + (dds ? "\nДоступные службы: " + mapper.writeValueAsString(services) : "")
                    + "\nТекущий черновик: " + mapper.writeValueAsString(command.draft())));
            for (var message : command.messages()) {
                messages.add(new IncidentLanguageModelPort.Message(message.role(), message.content()));
            }
            var result = validator.validate(model.generate(messages), classifierNames.keySet(), services.keySet(), dds,
                    command.draft());
            if (dds) {
                var assignment = result.incident().path("initialAssignment").path("emergencyService");
                if (assignment.isTextual()) classifierCatalog.requireService(assignment.asText());
                for (var service : result.incident().path("preparedCardTemplate").path("assignedServices")) {
                    classifierCatalog.requireService(service.asText());
                }
                for (var stage : result.incident().path("stages")) for (var call : stage.path("calls")) {
                    if ("SERVICE".equals(call.path("counterparty").asText())) classifierCatalog.requireService(call.path("serviceCode").asText());
                }
            }
            return result;
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
