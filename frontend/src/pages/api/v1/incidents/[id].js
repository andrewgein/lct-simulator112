import { getIncident, updateIncident } from "../../../../features/incident/api/AdminIncidentApi";

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
