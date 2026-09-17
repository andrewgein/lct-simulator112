import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/context";

export async function createContext(levelId, token) {
    return apiCall(API_PREFIX, "POST", { levelId }, token);
}

export async function createCardForDialup(contextId, dialupId, context, token) {
    return await apiCall(`${API_PREFIX}/${contextId}/dialups/${dialupId}/cards`, "POST", context, token);
}

export async function saveCardRevision(contextId, cardId, context, token) {
    return await apiCall(`${API_PREFIX}/${contextId}/cards/${cardId}/revisions`, "POST", context, token);
}

export async function getCards(contextId, token) {
    return await apiCall(`${API_PREFIX}/${contextId}/cards`, "GET", undefined, token);
}

export async function closeContext(contextId, token) {
    return await apiCall(`${API_PREFIX}/${contextId}/close`, "POST", {}, token);
}
