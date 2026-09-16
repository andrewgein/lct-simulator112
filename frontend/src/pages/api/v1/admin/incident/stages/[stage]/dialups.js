import { apiCall } from "../../../../../../../services/ApiClient";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    return apiCall(`/api/v1/admin/incident/stages/${params.stage}/dialups`, "POST", await request.json(), cookies.get("accessToken")?.value);
}
