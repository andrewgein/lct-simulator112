package com.simulator112.classifier.application.service;

import com.simulator112.classifier.application.port.in.SearchClassifierEntriesUseCase;
import com.simulator112.classifier.application.port.out.ClassifierRepository;
import com.simulator112.classifier.domain.model.ClassifierCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassifierCandidateSearchService implements SearchClassifierEntriesUseCase {
    private final ClassifierRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassifierCandidate> search(String query, int limit, List<String> includedCodes) {
        if (query == null || query.length() > 4000 || limit < 1 || limit > 80
                || includedCodes == null || includedCodes.size() > 15) {
            throw new IllegalArgumentException("Некорректные параметры поиска классификатора");
        }
        var words = Arrays.stream(normalize(query).split("[^а-яa-z0-9]+"))
                .filter(word -> word.length() >= 4).distinct().toList();
        Set<String> included = new HashSet<>(includedCodes);
        var candidates = repository.findCandidates();
        var ranked = candidates.stream().filter(entry -> !included.contains(entry.code()))
                .map(entry -> new Ranked(entry, score(words, entry)))
                .sorted(Comparator.comparingInt(Ranked::score).reversed()
                        .thenComparing(rank -> rank.entry().code()))
                .toList();

        boolean hasMatches = ranked.stream().anyMatch(rank -> rank.score() > 0);
        var selected = ranked.stream().filter(rank -> !hasMatches || rank.score() > 0)
                .limit(hasMatches ? limit : Math.min(limit, 40)).map(Ranked::entry).toList();
        var pinned = candidates.stream().filter(entry -> included.contains(entry.code()))
                .sorted(Comparator.comparing(ClassifierCandidate::code)).toList();
        var result = new java.util.ArrayList<>(selected);
        result.addAll(pinned);
        return result;
    }

    private int score(List<String> queryWords, ClassifierCandidate entry) {
        var words = Arrays.stream(normalize(entry.categoryName() + " " + entry.finalName()).split("[^а-яa-z0-9]+"))
                .filter(word -> word.length() >= 4).toList();
        return queryWords.stream().mapToInt(queryWord -> words.stream().mapToInt(word ->
                word.equals(queryWord) ? 6 : word.startsWith(queryWord.substring(0, Math.min(5, queryWord.length())))
                        || queryWord.startsWith(word.substring(0, Math.min(5, word.length()))) ? 2 : 0)
                .max().orElse(0)).sum();
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.forLanguageTag("ru")).replace('ё', 'е');
    }

    private record Ranked(ClassifierCandidate entry, int score) {}
}
