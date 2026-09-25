package com.simulator112.classifier.application.port.in;

import com.simulator112.classifier.domain.model.ClassifierCandidate;
import java.util.List;

public interface SearchClassifierEntriesUseCase {
    List<ClassifierCandidate> search(String query, int limit, List<String> includedCodes);
}
