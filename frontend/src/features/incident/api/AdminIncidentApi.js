import { apiCall } from "../../../services/ApiClient";
import { findAvailableIncidents } from "./IncidentApi";

/** @typedef {import("../contract/Incident").IncidentRequest} IncidentRequest */

const API_PREFIX = "/api/v1/incidents";

/** @param {string} token */
export async function getIncidentLibrary(token) {
    const response = await findAvailableIncidents(null, null, token);
    if (!response.ok) throw new Error("Не удалось загрузить сценарии");
    return response.json();
}

/** @param {string} id @param {string} token */
export function getIncident(id, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(id)}`, "GET", undefined, token);
}

/** @param {IncidentRequest} incident @param {string} token */
export function createIncident(incident, token) {
    return apiCall(API_PREFIX, "POST", incident, token);
}

/** @param {string} id @param {IncidentRequest} incident @param {string} token */
export function updateIncident(id, incident, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(id)}`, "PUT", incident, token);
}
