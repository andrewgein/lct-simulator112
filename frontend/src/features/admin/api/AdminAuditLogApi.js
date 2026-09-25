import { apiCall } from "../../../services/ApiClient";

export async function searchAuditLogs(params, token) {
    const query = new URLSearchParams(Object.fromEntries(Object.entries(params).filter(([, v]) => v)));
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return await apiCall(`/api/v1/admin/audit-logs${suffix}`, "GET", undefined, token);
}
