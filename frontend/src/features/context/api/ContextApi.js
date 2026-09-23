import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/context";
const segment = encodeURIComponent;

/** @param {string} assignmentId @param {string} token */
export function createContext(assignmentId, token) {
    return apiCall(API_PREFIX, "POST", { assignmentId }, token);
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

/** @param {string} contextId @param {string} incidentId @param {{ serviceCode: string, status: import("../contract/Context").ReactionStatus, comment?: string }} body @param {string} token */
export function applyReactionStatus(contextId, incidentId, body, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/dds/incidents/${segment(incidentId)}/reaction-status`, "POST", body, token);
}

/** @param {string} contextId @param {string} token */
export function closeContext(contextId, token) {
    return apiCall(`${API_PREFIX}/${segment(contextId)}/close`, "POST", {}, token);
}
