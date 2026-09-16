import { apiCall } from "../../../services/ApiClient";

/** @typedef {import("../contract/Incident").IncidentRequest} IncidentRequest */

const API_PREFIX = "/api/v1/admin/incident"

export async function getIncidentLibrary(token) {
    const incidents = [];
    for (let page = 0; ; page++) {
        const response = await apiCall(`${API_PREFIX}?page=${page}&size=100`, "GET", undefined, token);
        if (!response.ok) throw new Error("Не удалось загрузить библиотеку происшествий");
        const data = await response.json();
        if (Array.isArray(data)) return data;
        incidents.push(...data.content);
        if (data.last || page + 1 >= data.totalPages) return incidents;
    }
}

/**
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function getAllIncidents(token) {
    return await apiCall(API_PREFIX, "GET", undefined, token);
}

/**
 * @param {string} id Incident UUID
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function getFullIncidentById(id, token) {
    return await apiCall(`${API_PREFIX}/${id}`, "GET", undefined, token);
}

/**
 * @param {IncidentRequest} incident
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function createIncident(incident, token) {
    return await apiCall(API_PREFIX, "POST", incident, token);
}

/**
 * @param {IncidentRequest & { id: string }} incident
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function updateIncident(incident, token) {
    return await apiCall(`${API_PREFIX}/${incident.id}`, "PATCH", incident, token);
}

/**
 * @param {{ id: string }} incident
 * @param {string} token
 * @returns {Promise<Response>}
 */
export async function deleteIncident(incident, token) {
    return await apiCall(`${API_PREFIX}/${incident.id}`, "DELETE", undefined, token);
}
