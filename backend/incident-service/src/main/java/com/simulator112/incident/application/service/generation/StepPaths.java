package com.simulator112.incident.application.service.generation;

import tools.jackson.databind.JsonNode;

import java.util.Set;
import java.util.function.BiConsumer;

public record StepPaths(Set<String> roots, String guidance, BiConsumer<JsonNode, String[]> validate) {
    public StepPaths {
        roots = Set.copyOf(roots);
    }

    public boolean matches(String path) {
        return path != null && path.startsWith("/") && roots.contains(path.substring(1).split("/", 2)[0]);
    }

    public void check(String path, JsonNode draft) {
        if (!matches(path)) throw new IllegalStateException("Path belongs to another step");
        validate.accept(draft, path.substring(1).split("/", -1));
    }

    public static int selectedIndex(String path) {
        return Integer.parseInt(path.split("/")[2]);
    }

    public static int existingIndex(String value, JsonNode array) {
        if (!value.matches("0|[1-9][0-9]*")) throw new IllegalStateException("Invalid patch index");
        int index;
        try { index = Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalStateException("Invalid patch index", e); }
        if (!array.isArray() || index >= array.size()) throw new IllegalStateException("Unknown patch index");
        return index;
    }
}
