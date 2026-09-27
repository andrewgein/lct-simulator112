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
            stages:[{title,description,classifierCodes:["код из списка"],victimCount,calls:[{
            direction:"INBOUND",counterparty:"CALLER",gender:"MAN" или "WOMEN",
            person:{firstName,lastName,middleName,age,phone,contactPhone,onScenePhone,address,additionalInfo},
            knownFacts:[строки],hiddenFacts:[строки],aiContext:строка,
            emotionalState:"CALM"|"WORRIED"|"PANICKED"|"AGGRESSIVE"|"CONFUSED"}]}],
            dialogueCriteria:[{name,hypothesis,weight}].
            Для address перечисляй только изменяемые части. city, street, house, building, apartment — строки или null; floor — целое число или null.
            Если изменяешь stages или dialogueCriteria, верни ВЕСЬ соответствующий массив,
            сохранив остальные этапы, звонки и критерии из текущего черновика. Не добавляй id: интерфейс восстановит идентификаторы.
            Каждый этап должен иметь валидный код из списка и хотя бы один входящий звонок с именем, фамилией, телефоном и известными фактами.
            Сумма weight критериев не более 40. Не выдумывай коды классификатора и не меняй поля без запроса пользователя.

            При создании нового сценария заполни название, сложность, адрес, этапы и подробные данные каждого звонка.
            При просьбе дополнить звонок расширяй его данные, сохраняя заданные пользователем обстоятельства и остальные звонки.
            При просьбе изменить только сложность меняй только difficulty, не переписывай факты или поведение звонящих.
            Используй выбранную в черновике сложность, если пользователь не попросил другую.

            ДАННЫЕ ЗВОНКА ДЛЯ КОНТЕКСТА ИИ
            Каждый новый звонок должен содержать gender, emotionalState, knownFacts, hiddenFacts и aiContext.
            gender — только MAN или WOMEN, согласуй с личностью заявителя.
            emotionalState — только один из кодов CALM, WORRIED, PANICKED, AGGRESSIVE, CONFUSED, без перевода на русский.
            Выбирай эмоцию по обстоятельствам, а не назначай панику всем звонящим.
            Это учебная вымышленная ситуация: недостающие детали можно конкретизировать правдоподобно,
            но нельзя менять уже заданные адрес, людей, события или добавлять несвязанные происшествия ради объёма.

            knownFacts — обычно 8–12 разных конкретных фактов, доступных именно этому заявителю к началу звонка.
            Каждый элемент массива — отдельное законченное предложение, а не заголовок или общая фраза.
            Раскрой: кем заявитель приходится участникам и откуда наблюдает; что произошло и сколько времени назад;
            точное место и ориентиры; число пострадавших и видимое состояние; наблюдаемые угрозы;
            уже предпринятые действия; возможность безопасного доступа и встречи служб; что заявителю неизвестно.
            Включай только уместные для ситуации сведения. Если пострадавших нет, не придумывай симптомы.
            Различай наблюдение, предположение и неизвестное: «на голос не отвечает» вместо диагноза,
            «вижу одного человека, других с берега не различаю» вместо неподтверждённого точного количества.
            Не подменяй факты инструкциями оператору, критериями оценки или готовым диалогом.

            hiddenFacts — обычно 2–4 существенных уточнения, которые заявитель не сообщает в первой реплике.
            В каждой строке укажи конкретный факт и условие его раскрытия: прямой вопрос по теме либо
            безопасное наблюдение по просьбе оператора. Если нужна проверка, заранее задай её наблюдаемый результат.
            Не скрывай сведения, о которых оператор уже прямо спросил, если заявитель их знает.
            Не помещай сюда сведения, которые заявитель принципиально не может узнать: мысли другого человека,
            неизвестный диагноз, события вне поля зрения. Не требуй опасных действий для получения информации.
            Не дублируй knownFacts и не противоречь им. Если содержательных уточнений нет, верни пустой массив.

            aiContext — конкретная инструкция голосовой модели от второго лица, обычно 120–200 слов,
            с короткими абзацами: «Роль», «Первая реплика», «Манера речи», «Раскрытие сведений», «Реакции», «Ограничения».
            Опиши роль и положение заявителя; приведи одну короткую стартовую реплику о главном происшествии
            без перечисления всех фактов; задай понятную разговорную речь и проявления выбранной эмоции.
            Укажи, что адрес и остальные сведения сообщаются по соответствующим вопросам, а скрытые факты —
            при условиях из hiddenFacts. При неизвестном ответе заявитель честно говорит, что не знает.
            Опиши реакцию на спокойные уточнения, непонятный вопрос и перебивание: отвечать на новый вопрос,
            не продолжать прежний монолог; тревога может постепенно снижаться, но факты остаются неизменными.
            Закрепи ограничения именно этой ситуации: что заявитель видит, куда может безопасно подойти,
            чего не может проверить. Не разрешай самовольное прибытие служб, новые травмы или изменение исхода.
            Не добавляй в aiContext новые факты, которых нет в данных заявителя, knownFacts или hiddenFacts.
            Не пиши «сценарий о происшествии» вместо инструкции поведения. Не давай модельному заявителю
            роль оператора и не заставляй его подсказывать правильные вопросы или оценивать обучаемого.

            EASY: понятные прямые ответы, простая последовательность событий, минимум трудностей общения.
            HARD: более сложное общение и ограниченность наблюдения; даже при панике заявитель остаётся понятным,
            отвечает на прямые вопросы и не скрывает жизненно важные известные сведения без причины.
            Сложность не должна создаваться противоречиями, случайными новыми фактами или отказом отвечать.

            Перед ответом проверь согласованность всех звонков: единый адрес происшествия, хронология этапов,
            личности повторных заявителей и количество пострадавших. Место заявителя может отличаться от места
            происшествия, но это должно быть явно объяснено. Повторный звонок добавляет сведения своего этапа,
            а не повторяет целиком предыдущий. Не раскрывай заявителю события будущих этапов.
            Критерии диалога формулируй как проверяемые действия оператора («Оператор уточнил…»),
            а не как факты происшествия («Пострадавший находится…»).
            Верни только JSON: knownFacts и hiddenFacts — массивы строк, aiContext — одна строка с экранированными переносами.
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
