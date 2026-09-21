import { apiCall } from "../../../services/ApiClient";

const INCIDENTS_ENDPOINT = "/api/v1/incidents";
const CLASSIFIER_ENDPOINT = "/api/v1/classifier";

/**
 * @param {import("../contract/Incident").IncidentTargetType} targetType
 * @param {import("../contract/Incident").Difficulty} difficulty
 * @param {string} token
 */
export function findAvailableIncidents(targetType, difficulty, token) {
    const query = new URLSearchParams({ targetType, difficulty });
    return apiCall(`${INCIDENTS_ENDPOINT}?${query}`, "GET", undefined, token);
}

/** @param {string} incidentId @param {string} token */
export function getIncident(incidentId, token) {
    return apiCall(`${INCIDENTS_ENDPOINT}/${encodeURIComponent(incidentId)}`, "GET", undefined, token);
}

/** @param {string} token */
export function getIncidentClassifier(token) {
    return apiCall(CLASSIFIER_ENDPOINT, "GET", undefined, token);
}

/** @param {string} classifierCode @param {Object.<string, string>} facts @param {string} token */
export function resolveRouting(classifierCode, facts, token) {
    return apiCall(`${CLASSIFIER_ENDPOINT}/${encodeURIComponent(classifierCode)}/routing`, "POST", { facts }, token);
}
