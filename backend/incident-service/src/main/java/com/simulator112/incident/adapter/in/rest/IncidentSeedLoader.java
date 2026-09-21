package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Incident;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IncidentSeedLoader {
    private static final String LOCATION = "classpath:incidents/*.json";

    private final ResourcePatternResolver resources;
    private final ObjectMapper objectMapper;
    private final IncidentRestMapper mapper;

    public List<Incident> load() {
        List<Incident> incidents = new ArrayList<>();
        for (Resource resource : seedFiles()) {
            List<IncidentRequest> requests = read(resource);
            for (int index = 0; index < requests.size(); index++) {
                IncidentRequest request = requests.get(index);
                try {
                    incidents.add(mapper.toDomain(null, request));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(resource.getFilename() + ", инцидент #" + (index + 1)
                            + " «" + request.title() + "»: " + exception.getMessage(), exception);
                }
            }
        }
        if (incidents.isEmpty()) {
            throw new IllegalStateException("Не найдено инцидентов в " + LOCATION);
        }
        return incidents;
    }

    private List<Resource> seedFiles() {
        try {
            return Arrays.stream(resources.getResources(LOCATION))
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Не удалось найти файлы инцидентов " + LOCATION, exception);
        }
    }

    private List<IncidentRequest> read(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        } catch (IOException | JacksonException exception) {
            throw new IllegalStateException("Некорректный файл инцидентов " + resource.getFilename()
                    + ": " + exception.getMessage(), exception);
        }
    }
}
