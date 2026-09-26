package com.simulator112.incident.application.port.out;

import java.util.List;

public interface ClassifierCatalogPort {
    void requireEntry(String classifierCode);

    void requireService(String serviceCode);

    List<Candidate> search(String query, int limit, List<String> includedCodes);

    record Candidate(String code, String categoryName, String finalName) {}
}
