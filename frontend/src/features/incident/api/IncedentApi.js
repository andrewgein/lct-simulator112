import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/incident";

/**
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function getAllLevels(token) {
    return apiCall(`${API_PREFIX}/levels`, "GET", undefined, token);
}

/**
 * @param {string} incidentId
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function getIncidentById(incidentId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(incidentId)}`, "GET", undefined, token);
}

/**
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function getIncidentClassifier(token) {
    return apiCall(`${API_PREFIX}/classifier`, "GET", undefined, token);
}
