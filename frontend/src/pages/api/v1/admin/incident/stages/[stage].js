import { apiCall } from "../../../../../../services/ApiClient";

export const prerender = false;

export async function PATCH({ params, request, cookies }) {
    return apiCall(`/api/v1/admin/incident/stages/${params.stage}`, "PATCH", await request.json(), cookies.get("accessToken")?.value);
}

export async function DELETE({ params, cookies }) {
    return apiCall(`/api/v1/admin/incident/stages/${params.stage}`, "DELETE", undefined, cookies.get("accessToken")?.value);
}
