import { apiCall } from "../../../services/ApiClient";

export async function getSettingsOverview(token) {
    return await apiCall("/api/v1/admin/settings/overview", "GET", undefined, token);
}
