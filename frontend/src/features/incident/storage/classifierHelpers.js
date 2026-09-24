function featureAt(entry, level) {
    return entry.features.find((feature) => feature.level === level);
}

function featureKey(entry, level) {
    return Array.from({ length: level }, (_, index) => entry[`feature${index + 1}Code`] || "").join(":");
}

function featureNames(entries, level) {
    const nameProperty = `feature${level}Name`;
    const codeProperty = `feature${level}Code`;
    return new Map(entries.filter((entry) => entry[codeProperty] && entry[nameProperty]).map((entry) => [featureKey(entry, level), entry[nameProperty]]));
}

function normalizeEntry(entry, category) {
    const feature1 = featureAt(entry, 1);
    const feature2 = featureAt(entry, 2);
    const feature3 = featureAt(entry, 3);
    return {
        ...entry,
        categoryCode: category.code,
        categoryName: category.name,
        feature1Code: feature1?.code || null,
        feature1Name: feature1?.name?.trim() || null,
        feature2Code: feature2?.code || null,
        feature2Name: feature2?.name?.trim() || null,
        feature3Code: feature3?.code || null,
        feature3Name: feature3?.name?.trim() || null,
        additionalFeatures: entry.additionalFeatures?.trim() || null,
        routingFactCodes: Array.isArray(entry.routingFactCodes) ? entry.routingFactCodes : [],
        fields: []
    };
}

export function normalizeClassifier(view) {
    if (!view || !Array.isArray(view.categories) || !Array.isArray(view.routingFacts)) {
        throw new Error("Некорректный формат классификатора");
    }
    const classifier = view.categories.map((category) => ({
        ...category,
        entries: normalizeEntries(category)
    }));
    return { classifier, routingFacts: view.routingFacts };
}

function normalizeEntries(category) {
    const entries = category.entries.map((entry) => normalizeEntry(entry, category));
    const names = [1, 2, 3].map((level) => featureNames(entries, level));
    return entries.map((entry) => ({
        ...entry,
        feature1Name: entry.feature1Name || names[0].get(featureKey(entry, 1)) || null,
        feature2Name: entry.feature2Name || names[1].get(featureKey(entry, 2)) || null,
        feature3Name: entry.feature3Name || names[2].get(featureKey(entry, 3)) || null
    }));
}
