import { getIncidentClassifier } from "../../../../features/incident/api/IncedentApi";

export const prerender = false;

export async function GET({ cookies }) {
    return await getIncidentClassifier(cookies.get("accessToken")?.value);
}
