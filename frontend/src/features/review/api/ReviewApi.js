import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/review";

/** @param {string} token @returns {Promise<Response>} */
export function getUserReviews(token) {
    return apiCall(API_PREFIX, "GET", undefined, token);
}

/** @param {string} studentId @param {string} token @returns {Promise<Response>} */
export function getStudentReviews(studentId, token) {
    return apiCall(`${API_PREFIX}/users/${encodeURIComponent(studentId)}`, "GET", undefined, token);
}

/** @param {string} contextId @param {string} token @returns {Promise<Response>} */
export function getReview(contextId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(contextId)}`, "GET", undefined, token);
}

/** @param {string} contextId @param {string} token @returns {Promise<Response>} */
export function getReviewComments(contextId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(contextId)}/comments`, "GET", undefined, token);
}

/** @param {string} contextId @param {string} text @param {string} token @returns {Promise<Response>} */
export function addReviewComment(contextId, text, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(contextId)}/comments`, "POST", { text }, token);
}
