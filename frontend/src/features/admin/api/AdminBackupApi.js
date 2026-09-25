import { apiCall } from "../../../services/ApiClient";

export async function listBackups(token) {
    return await apiCall("/api/v1/admin/backups", "GET", undefined, token);
}

export async function runBackup(token) {
    return await apiCall("/api/v1/admin/backups/run", "POST", {}, token);
}

export async function downloadBackup(id, token) {
    return await apiCall(`/api/v1/admin/backups/${encodeURIComponent(id)}/download`, "GET", undefined, token);
}
