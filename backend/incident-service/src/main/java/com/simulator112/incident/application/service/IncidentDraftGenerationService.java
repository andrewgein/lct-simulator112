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
            Создавай учебные сценарии Системы-112 на русском. Ответ — только JSON:
            {"message":"краткий ответ","incident":{изменяемые поля}}. Для ответа без изменений incident: {}.
            Допустимые поля incident:
            title:строка, difficulty:"EASY"|"NORMAL"|"HARD",
            address:{city,street,house,building,apartment,floor},
            stages:[{title,description,classifierCodes:["код из списка"],victimCount,calls:[{
            direction:"INBOUND",counterparty:"CALLER",gender:"MAN"|"WOMEN",
            person:{firstName,lastName,middleName,age,phone,contactPhone,onScenePhone,address,additionalInfo},
            knownFacts:[строки],hiddenFacts:[строки],aiContext:строка,
            emotionalState:"CALM"|"WORRIED"|"PANICKED"|"AGGRESSIVE"|"CONFUSED"}]}],
            dialogueCriteria:[{name,hypothesis,weight}].
            Адресные поля — строки или null, floor — неотрицательное целое или null;
            age — неотрицательное целое или null, victimCount — неотрицательное целое.

            Новый сценарий: заполни название, сложность, адрес, этапы и звонки. Без указаний об объёме —
            один этап с одним звонком; явно запрошенное число сохрани. Сложность бери из черновика,
            если пользователь не выбрал другую. При редактировании меняй только запрошенное.
            Просьба изменить только сложность: {"message":"Готово","incident":{"difficulty":"HARD"}}.
            Адрес возвращай только в изменённой части; stages и dialogueCriteria — целыми массивами,
            сохраняя остальные элементы. Не добавляй id. Каждый этап: код из списка и хотя бы один звонок
            с именем, фамилией, телефоном и известными фактами. Коды не выдумывай.

            Для каждого нового или дополняемого звонка:
            knownFacts — 4–6 коротких фактов, которые заявитель знает к началу звонка:
            что и где произошло, что видит, состояние людей, угрозы, доступ и выполненные действия.
            Он сообщает их по соответствующим вопросам без дополнительных условий и проверок.
            Только уместные сведения, без диагнозов и догадок. Один факт — одна строка массива.
            hiddenFacts — 0–2 ДРУГИХ факта, о которых заявитель не говорит сам. Просто факты, без условий
            и без слов «если оператор спросит». Один факт — одна непустая строка.
            Не повторяй knownFacts даже другими словами; не добавляй недоступные заявителю сведения.
            Если подходящих фактов нет — верни ровно []. Пустые строки и null в массивах запрещены.
            aiContext — 3–5 коротких предложений ТОЛЬКО о том, как говорить и реагировать:
            тон, темп, эмоции, реакция на уточнения и перебивание. Отвечать на заданный вопрос,
            не выдавать всё сразу и не подсказывать оператору. При неизвестном ответе честно сказать об этом.
            Не пересказывай факты и личные данные из knownFacts и hiddenFacts, не добавляй новые события
            и готовую первую реплику с описанием происшествия.
            Перед ответом убери смысловые повторы: каждый факт должен быть только в одном списке,
            а aiContext должен содержать только правила поведения.
            Пол и эмоцию указывай кодами выше. EASY — чёткие ответы; NORMAL — волнение и уточнения;
            HARD — сложнее общение, но ответы понятны и непротиворечивы.

            Согласуй адрес, личности, хронологию и пострадавших во всех звонках. Недостающие детали
            учебной ситуации уточняй правдоподобно, сохраняя заданное пользователем. Без лишних событий.
            Критерии — действия оператора («Оператор уточнил…»), name и hypothesis непустые,
            weight — целое от 1 до 40, сумма не более 40. Не добавляй критерии без запроса. Пиши компактный JSON без отступов и пояснений.
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
            candidates = classifierCatalog.search(searchQuery, 40, includedCodes);
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
            try {
                return generateAndValidate(messages, classifierNames, classifierFailure);
            } catch (IncidentGenerationException e) {
                if (e.getClass() != IncidentGenerationException.class) throw e;
                return generateAndValidate(messages, classifierNames, classifierFailure);
            }
        } catch (IncidentGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось подготовить запрос генерации", e);
        }
    }

    private Result generateAndValidate(List<IncidentLanguageModelPort.Message> messages,
            LinkedHashMap<String, String> classifierNames, Exception classifierFailure) {
        var content = model.generate(messages);
        if (mapper.readTree(content).path("incident").has("stages") && classifierNames.isEmpty()) {
            throw new ClassifierUnavailableException(classifierFailure);
        }
        return validator.validate(content, classifierNames.keySet());
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
