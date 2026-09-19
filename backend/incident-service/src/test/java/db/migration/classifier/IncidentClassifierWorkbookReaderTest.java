package db.migration.classifier;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentClassifierWorkbookReaderTest {

    @Test
    void readsCompleteClassifier() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("classifier/incident-classifier-v046-24.xlsx")) {
            assertThat(inputStream).isNotNull();

            var classifier = new IncidentClassifierWorkbookReader().read(inputStream);

            assertThat(classifier.categories()).hasSize(24);
            assertThat(classifier.entries()).hasSize(1283);
            assertThat(classifier.services()).hasSize(58);
            assertThat(classifier.routingVariants()).hasSize(86);
            assertThat(classifier.routingRules()).hasSize(22484);
            assertThat(classifier.services()).extracting(
                    IncidentClassifierWorkbookReader.ServiceData::code
            ).contains("MCHS", "POLICE", "AMBULANCE", "MOSGAZ");
            assertThat(classifier.entries()).anySatisfy(entry -> {
                assertThat(entry.code()).isEqualTo("1010101");
                assertThat(entry.finalName()).isEqualTo("пожар: мусор");
            });
            assertThat(classifier.routingRules()).anySatisfy(rule -> {
                assertThat(rule.entryCode()).isEqualTo("1010101");
                assertThat(rule.sourceColumn()).isEqualTo("N");
                assertThat(rule.targetTypeName()).isEqualTo("пожар: мусор");
            });
            assertThat(classifier.routingVariants()).anySatisfy(variant -> {
                assertThat(variant.sourceColumn()).isEqualTo("N");
                assertThat(variant.routingTarget()).isEqualTo("Служба 101");
            });
        }
    }
}
