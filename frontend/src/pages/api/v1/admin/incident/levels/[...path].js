import { apiCall } from "../../../../../../services/ApiClient";

export const prerender = false;

export async function ALL({ params, request, cookies }) {
    const path = params.path || "";
    const levelId = /^[0-9a-fA-F-]{36}$/.test(path);
    const allowedMethods = path === "" ? ["GET", "POST"] : levelId ? ["GET", "PUT", "DELETE"] : [];
    if (!allowedMethods.includes(request.method)) {
        return new Response(null, { status: 405 });
    }
    const body = ["POST", "PUT"].includes(request.method) ? await request.json() : undefined;
    return apiCall(
        `/api/v1/admin/incident/levels${path ? `/${path}` : ""}`,
        request.method,
        body,
        cookies.get("accessToken")?.value,
    );
}
