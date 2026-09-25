import { apiCall } from "../../../services/ApiClient";

export async function getMonitoringOverview(token) {
    return await apiCall("/api/v1/admin/monitoring/overview", "GET", undefined, token);
}
