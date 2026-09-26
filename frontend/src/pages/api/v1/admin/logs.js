import { getLogs } from "../../../../features/admin/api/AdminLogsApi";

export const prerender = false;

export async function GET({ url, cookies }) {
    if (cookies.get("role")?.value !== "ADMIN") {
        return new Response(null, { status: 403 });
    }
    const service = url.searchParams.get("service");
    const since = url.searchParams.get("since");
    if (!service) {
        return new Response(JSON.stringify({ message: "service is required" }), { status: 400, headers: { "Content-Type": "application/json" } });
    }
    return getLogs(service, cookies.get("accessToken")?.value, since);
}
