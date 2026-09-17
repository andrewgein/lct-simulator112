import { apiCall } from "../../../../../../services/ApiClient";

export const prerender = false;

// Forward only the level endpoints supported by the backend.
export async function ALL({ params, request, cookies }) {
    const path = params.path || "";
    const id = "[0-9a-fA-F-]{36}";
    const collection = path === "";
    const level = new RegExp(`^${id}$`).test(path);
    const attachment = new RegExp(`^${id}/incidents/${id}$`).test(path);
    const allowed = collection ? ["GET", "POST"] : level ? ["GET", "PATCH", "DELETE"] : attachment ? ["PUT", "DELETE"] : [];
    if (!allowed.includes(request.method)) return new Response(null, { status: 405 });
    const body = ["POST", "PATCH"].includes(request.method) ? await request.json() : undefined;
    return apiCall(`/api/v1/admin/incident/levels${path ? `/${path}` : ""}`, request.method, body, cookies.get("accessToken")?.value);
}
