import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/review";

export async function getUserReviews(token) {
    return await apiCall(API_PREFIX, "GET", undefined, token);
}

export async function getReview(contextId, token) {
    return await apiCall(`${API_PREFIX}/${contextId}`, "GET", undefined, token);
}
