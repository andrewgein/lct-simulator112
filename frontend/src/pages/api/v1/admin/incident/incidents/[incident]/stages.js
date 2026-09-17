import { apiCall } from "../../../../../../../services/ApiClient";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    return apiCall(`/api/v1/admin/incident/incidents/${params.incident}/stages`, "POST", await request.json(), cookies.get("accessToken")?.value);
}
