import { getIncidentClassifier } from "../../../features/incident/api/IncidentApi";

export const prerender = false;

export async function GET({ cookies }) {
    return await getIncidentClassifier(cookies.get("accessToken")?.value);
}
