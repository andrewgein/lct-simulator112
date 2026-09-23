import { createReactiveState } from "../../../../public/state";
import { normalizeClassifier } from "./classifierHelpers";

/** @typedef {import("../../incident/contract/Classifier").IncidentClassifier} IncidentClassifier */
export const classifierInfo = createReactiveState({
    classifier: [],
    loading: false,
    error: ""
});

let classifierRequest;

export function loadClassifier() {
    if (classifierInfo.state.classifier.length) {
        return Promise.resolve(classifierInfo.state.classifier);
    }
    classifierRequest ||= fetch("/api/v1/classifier")
        .then((response) => {
            if (!response.ok) throw new Error("Не удалось загрузить классификатор");
            return response.json();
        })
        .then((classifier) => {
            if (!Array.isArray(classifier)) throw new Error("Некорректный формат классификатора");
            const typedClassifier = /** @type {IncidentClassifier} */ (normalizeClassifier(classifier));
            classifierInfo.state.classifier = typedClassifier;
            classifierInfo.state.error = "";
            return typedClassifier;
        })
        .catch((error) => {
            classifierInfo.state.error = error.message;
            classifierRequest = undefined;
            throw error;
        })
        .finally(() => {
            classifierInfo.state.loading = false;
        });
    classifierInfo.state.loading = true;
    return classifierRequest;
}
