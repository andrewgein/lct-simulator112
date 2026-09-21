import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/context";
const segment = encodeURIComponent;

/** @param {string} levelId @param {string} token */
export function createContext(levelId, token) {
    return apiCall(API_PREFIX, "POST", { levelId }, token);
}

/** @param {string} contextId @param {string} callId @param {import("../contract/Context").SolutionCardRequest} card @param {string} token */
export function createCardForCall(contextId, callId, card, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/calls/${segment(callId)}/cards`, "POST", card, token);
}

/** @param {string} contextId @param {string} cardId @param {import("../contract/Context").SolutionCardRequest} card @param {string} token */
export function saveCardRevision(contextId, cardId, card, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/cards/${segment(cardId)}/revisions`, "POST", card, token);
}

/** @param {string} contextId @param {string} token */
export function getCards(contextId, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/cards`, "GET", undefined, token);
}

/** @param {string} contextId @param {string} token */
export function getLevelProgress(contextId, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/progress`, "GET", undefined, token);
}

/** @param {string} contextId @param {string} incidentId @param {import("../contract/Context").DdsStageSignal} signal @param {string} token */
export function applyDdsStageSignal(contextId, incidentId, signal, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/dds/incidents/${segment(incidentId)}/signals`, "POST", { signal }, token);
}

/** @param {string} contextId @param {string} token */
export function closeContext(contextId, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/close`, "POST", {}, token);
}
