import { apiCall } from "../../../services/ApiClient";

export async function listOpsServices(token) {
    return await apiCall("/api/v1/admin/ops/services", "GET", undefined, token);
}

export async function actOnService(service, action, token) {
    return await apiCall(
        `/api/v1/admin/ops/services/${encodeURIComponent(service)}/${encodeURIComponent(action)}`,
        "POST",
        {},
        token
    );
}
