package com.simulator112.review_service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureDependencyTests {
    private static final Path SOURCES = Path.of("src/main/java/com/simulator112/review_service");

    @Test
    void applicationDoesNotDependOnAdaptersTransportOrJpa() throws IOException {
        assertNoImports(SOURCES.resolve("application"), List.of("review_service.adapter", ".grpc.contract",
                "jakarta.persistence", "org.springframework.data"));
    }

    @Test
    void domainDoesNotDependOnApplicationAdaptersOrFrameworks() throws IOException {
        assertNoImports(SOURCES.resolve("domain"), List.of("review_service.application", "review_service.adapter",
                ".grpc.contract", "jakarta.persistence", "org.springframework"));
    }

    @Test
    void inboundAdaptersDoNotDependOnApplicationServices() throws IOException {
        assertNoImports(SOURCES.resolve("adapter/in"), List.of("review_service.application.service"));
    }

    private void assertNoImports(Path root, List<String> forbidden) throws IOException {
        try (var files = Files.walk(root)) {
            var violations = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> lines(path).stream().filter(line -> line.startsWith("import "))
                            .filter(line -> forbidden.stream().anyMatch(line::contains)).map(line -> path + ": " + line))
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    private List<String> lines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
