import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/review";

/** @param {string} token @returns {Promise<Response>} */
export function getUserReviews(token) {
    return apiCall(API_PREFIX, "GET", undefined, token);
}

/** @param {string} contextId @param {string} token @returns {Promise<Response>} */
export function getReview(contextId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(contextId)}`, "GET", undefined, token);
}
