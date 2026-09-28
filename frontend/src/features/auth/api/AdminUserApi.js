import { apiCall } from "../../../services/ApiClient";

const ADMIN_USERS_PREFIX = "/api/v1/admin/users";

export async function getAllUsers(token) {
    return await apiCall(ADMIN_USERS_PREFIX, "GET", undefined, token);
}

export async function updateUserRole(userId, role, token) {
    return await apiCall(`/api/v1/auth/change-role/${encodeURIComponent(userId)}`, "POST", { role }, token);
}

export function deleteUser(userId, token) {
    return apiCall(`${ADMIN_USERS_PREFIX}/${encodeURIComponent(userId)}`, "DELETE", undefined, token);
}
