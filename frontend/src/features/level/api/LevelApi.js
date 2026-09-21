import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/levels";

/** @param {string} levelId @param {string} token */
export function getLevel(levelId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(levelId)}`, "GET", undefined, token);
}

/** @param {import("../../incident/contract/Incident").LevelRequest} level @param {string} token */
export function createLevel(level, token) {
    return apiCall(API_PREFIX, "POST", level, token);
}

/** @param {string} levelId @param {import("../../incident/contract/Incident").LevelRequest} level @param {string} token */
export function updateLevel(levelId, level, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(levelId)}`, "PUT", level, token);
}
