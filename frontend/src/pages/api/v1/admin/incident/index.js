import { createIncident, getAllIncidents } from "../../../../../features/incident/api/AdminIncidentApi";

export const prerender = false;

export async function GET({ cookies }) {
    return await getAllIncidents(cookies.get("accessToken")?.value);
}

export async function POST({ request, cookies }) {
    return await createIncident(await request.json(), cookies.get("accessToken")?.value);
}
