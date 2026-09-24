import { resolveRouting } from "../../../../../features/incident/api/IncidentApi";

export const prerender = false;

export async function POST({ params, request, cookies }) {
    return await resolveRouting(
        params.code,
        (await request.json()).facts || {},
        cookies.get("accessToken")?.value
    );
}
