function additionalInfoField(entry) {
    return {
        id: `classifier_${entry.code}_additional_feature`,
        name: entry.additionalFeatures.trim(),
        type: "boolean",
        required: false
    };
}

export function normalizeClassifier(classifier) {
    return classifier.map((category) => ({
        ...category,
        entries: category.entries.map((entry) => ({
            ...entry,
            fields: Array.isArray(entry.fields) && entry.fields.length ? entry.fields : entry.additionalFeatures?.trim() ? [additionalInfoField(entry)] : []
        }))
    }));
}
