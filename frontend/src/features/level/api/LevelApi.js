import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/levels";

/** @param {string} token */
export function getLevels(token) {
    return apiCall(API_PREFIX, "GET", undefined, token);
}

/** @param {string} levelId @param {string} token */
export function getLevel(levelId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(levelId)}`, "GET", undefined, token);
}
