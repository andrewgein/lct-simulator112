package com.simulator112.incident.application.service.generation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

public final class SelectedFields {
    private SelectedFields() {}

    public static ObjectNode apply(ObjectMapper mapper, JsonNode draft, JsonNode generated, List<String> paths) {
        var patch = mapper.createObjectNode();
        for (var path : paths) {
            var value = generated.at(path);
            if (value.isMissingNode()) throw new IllegalStateException("Missing selected field: " + path);
            if (!changes(value, draft.at(path))) throw new IllegalStateException("Selected field was not changed: " + path);
            var parts = path.substring(1).split("/");
            var root = parts[0];
            if (parts.length == 1) {
                var copy = value.deepCopy();
                if ("stages".equals(root) && copy instanceof ArrayNode stages) {
                    for (var stage : stages) if (stage instanceof ObjectNode object) stripGeneratedIds(object);
                }
                patch.set(root, copy);
                continue;
            }
            if (!patch.has(root)) {
                patch.set(root, "stages".equals(root) || "dialogueCriteria".equals(root)
                        ? draft.path(root).deepCopy() : mapper.createObjectNode());
            }
            JsonNode target = patch.path(root);
            for (int i = 1; i < parts.length - 1; i++) {
                if (target instanceof ArrayNode array) {
                    target = array.get(index(parts[i], array));
                    continue;
                }
                if (!(target instanceof ObjectNode object)) throw new IllegalStateException("Invalid patch path");
                var child = object.path(parts[i]);
                if (child.isMissingNode()) {
                    child = draft.at("/" + String.join("/", java.util.Arrays.copyOfRange(parts, 0, i + 1))).deepCopy();
                    if (child.isMissingNode() || child.isNull()) child = mapper.createObjectNode();
                    object.set(parts[i], child);
                }
                target = child;
            }
            String last = parts[parts.length - 1];
            if (target instanceof ArrayNode array) {
                int position = index(last, array);
                var copy = value.deepCopy();
                if ("stages".equals(root) && parts.length == 2 && copy instanceof ObjectNode stage)
                    restoreStageIds(stage, draft.path("stages").get(position));
                if ("stages".equals(root) && parts.length == 4 && "calls".equals(parts[2])
                        && copy instanceof ObjectNode call)
                    restoreCallId(call, draft.path("stages").path(parts[1]).path("calls").get(position));
                array.set(position, copy);
            } else if (target instanceof ObjectNode object) {
                var copy = value.deepCopy();
                if ("stages".equals(root) && "calls".equals(last) && copy instanceof ArrayNode calls) {
                    var oldCalls = draft.at(path);
                    for (int i = 0; i < calls.size(); i++) if (calls.get(i) instanceof ObjectNode call)
                        restoreCallId(call, oldCalls.path(i));
                }
                object.set(last, copy);
            } else throw new IllegalStateException("Invalid patch parent: " + path);
        }
        if (patch.path("stages") instanceof ArrayNode stages && !paths.contains("/stages"))
            restoreIds(stages, draft.path("stages"));
        return patch;
    }

    private static boolean changes(JsonNode generated, JsonNode previous) {
        if (generated.isObject()) {
            for (var field : generated.properties())
                if (changes(field.getValue(), previous.path(field.getKey()))) return true;
            return false;
        }
        return !generated.equals(previous);
    }

    private static int index(String value, JsonNode array) {
        if (!value.matches("0|[1-9][0-9]*")) throw new IllegalStateException("Invalid patch index");
        int index;
        try { index = Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalStateException("Invalid patch index", e); }
        if (!array.isArray() || index >= array.size()) throw new IllegalStateException("Unknown patch index");
        return index;
    }

    private static void restoreIds(ArrayNode generated, JsonNode original) {
        for (int i = 0; i < generated.size(); i++) if (generated.get(i) instanceof ObjectNode stage)
            restoreStageIds(stage, original.path(i));
    }

    private static void restoreStageIds(ObjectNode stage, JsonNode original) {
        stage.remove(List.of("id", "position"));
        if (original.path("id").isTextual() && original.path("type").asText().equals(stage.path("type").asText()))
            stage.put("id", original.path("id").asText());
        var calls = stage.path("calls");
        for (int i = 0; i < calls.size(); i++) if (calls.get(i) instanceof ObjectNode call)
            restoreCallId(call, original.path("calls").path(i));
    }

    private static void restoreCallId(ObjectNode call, JsonNode original) {
        call.remove(List.of("id", "position"));
        if (original.path("id").isTextual()
                && original.path("counterparty").asText().equals(call.path("counterparty").asText()))
            call.put("id", original.path("id").asText());
    }

    private static void stripGeneratedIds(ObjectNode stage) {
        stage.remove(List.of("id", "position"));
        for (var call : stage.path("calls")) if (call instanceof ObjectNode object)
            object.remove(List.of("id", "position"));
    }
}
