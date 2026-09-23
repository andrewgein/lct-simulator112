function additionalInfoField(entry) {
    return {
        id: `classifier_${entry.code}_additional_info`,
        name: "Дополнительные сведения",
        type: "textarea",
        required: false,
        hint: entry.additionalFeatures || ""
    };
}

export function normalizeClassifier(classifier) {
    return classifier.map((category) => ({
        ...category,
        entries: category.entries.map((entry) => ({
            ...entry,
            fields: Array.isArray(entry.fields) && entry.fields.length ? entry.fields : [additionalInfoField(entry)]
        }))
    }));
}
