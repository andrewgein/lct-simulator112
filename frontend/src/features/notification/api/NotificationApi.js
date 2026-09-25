import { apiCall } from "../../../services/ApiClient";

const API_PREFIX = "/api/v1/notifications";

/** @param {string} token @returns {Promise<Response>} */
export function getNotifications(token) {
    return apiCall(API_PREFIX, "GET", undefined, token);
}

/** @param {string} token @returns {Promise<Response>} */
export function getUnreadCount(token) {
    return apiCall(`${API_PREFIX}/unread-count`, "GET", undefined, token);
}

/** @param {string} notificationId @param {string} token @returns {Promise<Response>} */
export function markNotificationRead(notificationId, token) {
    return apiCall(`${API_PREFIX}/${encodeURIComponent(notificationId)}/read`, "PATCH", undefined, token);
}

/** @param {string} token @returns {Promise<Response>} */
export function markAllNotificationsRead(token) {
    return apiCall(`${API_PREFIX}/read-all`, "PATCH", undefined, token);
}
