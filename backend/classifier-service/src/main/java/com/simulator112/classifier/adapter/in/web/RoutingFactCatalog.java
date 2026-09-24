package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.RoutingFactOptionView;
import com.simulator112.classifier.adapter.in.web.dto.RoutingFactView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.simulator112.classifier.adapter.in.web.dto.RoutingFactView.RoutingFactControlType.BOOLEAN;
import static com.simulator112.classifier.adapter.in.web.dto.RoutingFactView.RoutingFactControlType.SINGLE_SELECT;

final class RoutingFactCatalog {

    private static final Map<String, RoutingFactView> FACTS = facts();

    private RoutingFactCatalog() {
    }

    static List<RoutingFactView> definitions(Set<String> codes) {
        List<String> unknown = codes.stream().filter(code -> !FACTS.containsKey(code)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new IllegalStateException("Не описаны факты маршрутизации: " + String.join(", ", unknown));
        }
        return FACTS.values().stream().filter(fact -> codes.contains(fact.code())).toList();
    }

    private static Map<String, RoutingFactView> facts() {
        Map<String, RoutingFactView> facts = new LinkedHashMap<>();
        add(facts, fact("ACCESS_STATUS", "Доступ", SINGLE_SELECT,
                option("AVAILABLE", "Есть доступ"), option("NO_ACCESS", "Нет доступа")));
        add(facts, yesNo("THREAT_TO_PEOPLE", "Есть угроза людям"));
        add(facts, fact("CASUALTY_STATUS", "Есть пострадавшие или погибшие", BOOLEAN,
                option("PRESENT", "Да"), option("NONE", "Нет")));
        add(facts, fact("OFFENSE_STATUS", "Есть признаки правонарушения", BOOLEAN,
                option("PRESENT", "Да"), option("NONE", "Нет")));
        add(facts, fact("VICTIM_STATUS", "Пострадавшие", SINGLE_SELECT,
                option("PRESENT", "На месте"), option("NONE", "Нет"),
                option("NOT_ON_SCENE", "Не на месте")));
        add(facts, yesNo("GASIFICATION", "Объект газифицирован"));
        add(facts, yesNo("MEDICAL_HELP_REQUIRED", "Требуется медицинская помощь"));
        add(facts, yesNo("EVACUATION_REQUIRED", "Требуется эвакуация"));
        add(facts, yesNo("LARGE_GROUP_OR_OD", "Более пяти человек / ОД"));
        add(facts, yesNo("TRAFFIC_BLOCKED", "Есть перекрытие движения"));
        add(facts, fact("LOCATION_KIND", "Тип места", SINGLE_SELECT,
                option("TUNNEL", "Тоннель"), option("OTHER", "Другое")));
        add(facts, fact("ROAD_USER_KIND", "Участник дорожного движения", SINGLE_SELECT,
                option("PEDESTRIAN", "Пешеход"), option("VEHICLE", "Транспорт"),
                option("OTHER", "Другой")));
        add(facts, yesNo("COMMUNICATION_FACILITY", "Объект связи"));
        add(facts, yesNo("CONSTRUCTION_SITE", "Строительная площадка"));
        add(facts, yesNo("LISTED_OBJECT", "Объект из перечня"));
        add(facts, yesNo("POLYGON_EVENT", "Событие на полигоне"));
        add(facts, fact("LOCATION", "Местоположение", SINGLE_SELECT,
                option("MOSCOW", "Москва"), option("OTHER", "За пределами Москвы")));
        return Collections.unmodifiableMap(new LinkedHashMap<>(facts));
    }

    private static RoutingFactView yesNo(String code, String label) {
        return fact(code, label, BOOLEAN, option("TRUE", "Да"), option("FALSE", "Нет"));
    }

    private static RoutingFactView fact(
            String code,
            String label,
            RoutingFactView.RoutingFactControlType controlType,
            RoutingFactOptionView... options
    ) {
        return new RoutingFactView(code, label, controlType, List.of(options));
    }

    private static RoutingFactOptionView option(String value, String label) {
        return new RoutingFactOptionView(value, label);
    }

    private static void add(Map<String, RoutingFactView> facts, RoutingFactView fact) {
        facts.put(fact.code(), fact);
    }
}
