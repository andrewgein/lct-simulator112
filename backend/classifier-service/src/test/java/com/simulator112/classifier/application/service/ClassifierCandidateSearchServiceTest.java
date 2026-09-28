package com.simulator112.classifier.application.service;

import com.simulator112.classifier.application.port.out.ClassifierRepository;
import com.simulator112.classifier.domain.model.ClassifierCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassifierCandidateSearchServiceTest {
    private final ClassifierRepository repository = mock(ClassifierRepository.class);
    private final ClassifierCandidateSearchService search = new ClassifierCandidateSearchService(repository);

    @Test
    void ranksByNameAndPinsExistingCodesWithoutSendingCatalog() {
        when(repository.findCandidates()).thenReturn(List.of(
                new ClassifierCandidate("01", "Медицина", "Травма"),
                new ClassifierCandidate("02", "Пожары", "Пожар в квартире"),
                new ClassifierCandidate("03", "Пожары", "Дым в здании")));
        assertThat(search.search("Пожар в квартире", 1, List.of("03")))
                .extracting(ClassifierCandidate::code).containsExactly("02", "03");
    }

    @Test
    void ignoresShortCatalogWordsWhenRankingVerboseScenarioRequest() {
        when(repository.findCandidates()).thenReturn(List.of(
                new ClassifierCandidate("14040300", "Аварии в городском хозяйстве", "Вода с запахом"),
                new ClassifierCandidate("1050102", "Пожары и задымления", "задымление: квартира")));
        assertThat(search.search("Создай сценарий ДДС о задымлении квартиры; заявитель соседка, "
                        + "оператор связывается с бригадой", 1, List.of()))
                .extracting(ClassifierCandidate::code).containsExactly("1050102");
    }

    @Test
    void handlesEmptyDraftAndNoMatchesDeterministically() {
        when(repository.findCandidates()).thenReturn(List.of(
                new ClassifierCandidate("02", "Пожары", "Пожар"),
                new ClassifierCandidate("01", "Медицина", "Травма")));
        assertThat(search.search("другое событие", 1, List.of()))
                .extracting(ClassifierCandidate::code).containsExactly("01");
    }

    @Test
    void limitsInput() {
        assertThatThrownBy(() -> search.search("abc", 81, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
