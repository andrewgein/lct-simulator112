import { createIncident } from "../../../../features/incident/api/AdminIncidentApi";
import { findAvailableIncidents } from "../../../../features/incident/api/IncidentApi";

export const prerender = false;

export function GET({ url, cookies }) {
    return findAvailableIncidents(
        url.searchParams.get("targetType"),
        url.searchParams.get("difficulty"),
        cookies.get("accessToken")?.value,
    );
}

export async function POST({ request, cookies }) {
    return createIncident(await request.json(), cookies.get("accessToken")?.value);
}
