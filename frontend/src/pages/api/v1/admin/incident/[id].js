import { deleteIncident, getFullIncidentById, updateIncident } from "../../../../../features/incident/api/AdminIncidentApi";

export const prerender = false;

export async function GET({ params, cookies }) {
    return await getFullIncidentById(params.id, cookies.get("accessToken")?.value);
}

export async function PATCH({ params, request, cookies }) {
    const incident = { ...await request.json(), id: params.id };
    return await updateIncident(incident, cookies.get("accessToken")?.value);
}

export async function DELETE({ params, cookies }) {
    return await deleteIncident({ id: params.id }, cookies.get("accessToken")?.value);
}
