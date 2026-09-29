import { deleteIncident, getIncident, updateIncident } from "../../../../features/incident/api/AdminIncidentApi";

export const prerender = false;

export function GET({ params, cookies }) {
    return getIncident(params.id, cookies.get("accessToken")?.value);
}

export async function PUT({ params, request, cookies }) {
    return updateIncident(
        params.id,
        await request.json(),
        cookies.get("accessToken")?.value,
    );
}

export function DELETE({ params, cookies, locals }) {
    if (cookies.get("role")?.value !== "ADMIN") return new Response(null, { status: 403 });
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(params.id || "")) {
        return new Response(null, { status: 400 });
    }
    return deleteIncident(params.id, locals.accessToken || cookies.get("accessToken")?.value);
}
