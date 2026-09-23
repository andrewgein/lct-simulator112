function featureKey(entry, level) {
    return Array.from({ length: level }, (_, index) => entry[`feature${index + 1}Code`] || "").join(":");
}

function featureNames(entries, level) {
    const nameProperty = `feature${level}Name`;
    const codeProperty = `feature${level}Code`;
    return new Map(entries.filter((entry) => entry[codeProperty] && entry[nameProperty]?.trim()).map((entry) => [featureKey(entry, level), entry[nameProperty].trim()]));
}

export function normalizeClassifier(classifier) {
    return classifier.map((category) => {
        const names = [1, 2, 3].map((level) => featureNames(category.entries, level));
        return {
            ...category,
            entries: category.entries.map((entry) => ({
                ...entry,
                feature1Name: entry.feature1Name?.trim() || names[0].get(featureKey(entry, 1)) || null,
                feature2Name: entry.feature2Name?.trim() || names[1].get(featureKey(entry, 2)) || null,
                feature3Name: entry.feature3Name?.trim() || names[2].get(featureKey(entry, 3)) || null,
                additionalFeatures: entry.additionalFeatures?.trim() || null,
                fields: Array.isArray(entry.fields) ? entry.fields : []
            }))
        };
    });
}
