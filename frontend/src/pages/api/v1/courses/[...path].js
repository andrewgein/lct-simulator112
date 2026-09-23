import { apiCall } from "../../../../services/ApiClient";

export const prerender = false;

export async function ALL({ params, request, cookies }) {
    const role = cookies.get("role")?.value || "";
    const isEditor = ["ADMIN", "SUPERVISOR"].includes(role);
    const path = params.path || "";
    const courseId = /^[0-9a-fA-F-]{36}$/.test(path);
    const download = /^materials\/[0-9a-fA-F-]{36}\/file$/.test(path);
    const upload = path === "material-files";

    if (upload && (!isEditor || request.method !== "POST")) return new Response(null, { status: isEditor ? 405 : 403 });
    if (download && request.method !== "GET") return new Response(null, { status: 405 });
    if (!upload && !download) {
        const allowed = path === "" ? ["GET", "POST"] : courseId ? ["GET", "PUT"] : [];
        if (!allowed.includes(request.method)) return new Response(null, { status: 405 });
        if (["POST", "PUT"].includes(request.method) && !isEditor) return new Response(null, { status: 403 });
    }

    let body;
    if (upload) body = await request.formData();
    else if (["POST", "PUT"].includes(request.method)) body = await request.json();
    const response = await apiCall(`/api/v1/courses${path ? `/${path}` : ""}`, request.method, body,
        cookies.get("accessToken")?.value);
    if (!download) return response;

    const headers = new Headers(response.headers);
    headers.delete("x-frame-options");
    return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
}
