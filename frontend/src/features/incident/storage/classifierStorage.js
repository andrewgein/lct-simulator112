import { createReactiveState } from "../../../../public/state";

/** @typedef {import("../../incident/contract/Classifier").IncidentClassifier} IncidentClassifier */
/** @typedef {import("../../incident/contract/Classifier").ClassifierEntry} ClassifierEntry */

export const classifierInfo = createReactiveState({
    classifier: {},
    loading: false,
    error: ""
});

let classifierRequest;

export function loadClassifier() {
    if (Object.keys(classifierInfo.state.classifier).length) {
        return Promise.resolve(classifierInfo.state.classifier);
    }
    classifierRequest ||= fetch("/api/v1/classifier")
        .then((response) => {
            if (!response.ok) throw new Error("Не удалось загрузить классификатор");
            return response.json();
        })
        .then((classifier) => {
            const typedClassifier = /** @type {IncidentClassifier} */ (classifier);
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

/**
 * @param {string} incidentId Incident type UUID
 * @returns {ClassifierEntry | undefined}
 */
export function getIncidentById(incidentId) {
    for (const incidents of Object.values(classifierInfo.state.classifier)) {
        for (const incident of Object.values(incidents)) {
            if (incident.id === incidentId) return incident;
        }
    }
}

/**
 * @param {string} incidentId Incident type UUID
 * @returns {string | undefined}
 */
export function getServiceIdByIncidentId(incidentId) {
    for (const [serviceId, incidents] of Object.entries(classifierInfo.state.classifier)) {
        if (Object.values(incidents).some((incident) => incident.id === incidentId)) return serviceId;
    }
}
