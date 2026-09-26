import { getDispatchServices } from "../../../../features/incident/api/IncidentApi";

export const prerender = false;

export function GET({ cookies }) {
    return getDispatchServices(cookies.get("accessToken")?.value);
}
