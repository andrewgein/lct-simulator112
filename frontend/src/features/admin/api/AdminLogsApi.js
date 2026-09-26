import { apiCall } from "../../../services/ApiClient";

export async function listLogServices(token) {
    return await apiCall("/api/v1/admin/logs/services", "GET", undefined, token);
}

export async function getLogs(service, token, since) {
    const query = new URLSearchParams({ service });
    if (since) query.set("since", since);
    return await apiCall(`/api/v1/admin/logs?${query.toString()}`, "GET", undefined, token);
}
