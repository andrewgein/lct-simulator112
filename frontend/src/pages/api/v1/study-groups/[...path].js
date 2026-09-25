import { apiCall } from "../../../../services/ApiClient";

export const prerender = false;

export async function ALL({ params, request, cookies }) {
    if (!["ADMIN", "SUPERVISOR"].includes(cookies.get("role")?.value || "")) {
        return new Response(null, { status: 403 });
    }
    const path = params.path || "";
    const isGroup = /^[0-9a-fA-F-]{36}$/.test(path);
    const isCourseList = /^[0-9a-fA-F-]{36}\/courses$/.test(path);
    const isAssignment = /^[0-9a-fA-F-]{36}\/courses\/[0-9a-fA-F-]{36}$/.test(path);
    const allowedMethods = path === "" ? ["GET", "POST"] : isGroup ? ["GET", "PUT", "DELETE"] : isCourseList ? ["GET"] : isAssignment ? ["POST", "DELETE"] : [];
    if (!allowedMethods.includes(request.method)) return new Response(null, { status: 405 });
    const body = ["POST", "PUT"].includes(request.method) && !isAssignment ? await request.json() : undefined;
    return apiCall(`/api/v1/study-groups${path ? `/${path}` : ""}`, request.method, body, cookies.get("accessToken")?.value);
}
