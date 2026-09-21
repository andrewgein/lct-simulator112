package com.simulator112.contextmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureDependencyTests {
    private static final Path SOURCES = Path.of("src/main/java/com/simulator112/contextmanager");

    @Test
    void applicationDoesNotDependOnAdaptersOrTransportAndPersistenceContracts() throws IOException {
        assertNoImports(SOURCES.resolve("application"), List.of(
                "com.simulator112.contextmanager.adapter",
                "com.simulator112.context.grpc.contract",
                "com.simulator112.incident.grpc.contract",
                "jakarta.persistence",
                "org.springframework.data"));
    }

    @Test
    void domainDoesNotDependOnApplicationAdaptersOrFrameworks() throws IOException {
        assertNoImports(SOURCES.resolve("domain"), List.of(
                "com.simulator112.contextmanager.application",
                "com.simulator112.contextmanager.adapter",
                ".grpc.contract",
                "jakarta.persistence",
                "org.springframework"));
    }

    @Test
    void inboundAdaptersDependOnInputPortsInsteadOfApplicationServices() throws IOException {
        assertNoImports(SOURCES.resolve("adapter/in"), List.of(
                "com.simulator112.contextmanager.application.service"));
    }

    private void assertNoImports(Path root, List<String> forbidden) throws IOException {
        try (var files = Files.walk(root)) {
            var violations = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> readLines(path).stream()
                            .filter(line -> line.startsWith("import "))
                            .filter(line -> forbidden.stream().anyMatch(line::contains))
                            .map(line -> path + ": " + line))
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    private List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
