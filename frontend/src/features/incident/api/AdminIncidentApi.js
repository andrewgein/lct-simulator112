import { apiCall } from "../../../services/ApiClient";
import { findAvailableIncidents } from "./IncidentApi";

/** @typedef {import("../contract/Incident").IncidentRequest} IncidentRequest */

const API_PREFIX = "/api/v1/incidents";
const TARGET_TYPES = ["SYSTEM_112", "DDS"];
const DIFFICULTIES = ["EASY", "NORMAL", "HARD"];

/** @param {string} token */
export async function getIncidentLibrary(token) {
    const responses = await Promise.all(
        TARGET_TYPES.flatMap((targetType) => DIFFICULTIES.map((difficulty) =>
            findAvailableIncidents(targetType, difficulty, token)))
    );
    if (responses.some((response) => !response.ok)) {
        throw new Error("Не удалось загрузить библиотеку происшествий");
    }
    const incidents = (await Promise.all(responses.map((response) => response.json()))).flat();
    return [...new Map(incidents.map((incident) => [incident.id, incident])).values()];
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
